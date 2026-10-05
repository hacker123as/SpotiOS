package com.project.lol.webview.helpers

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Tampermonkey-style user scripts for the Spotify page (Dev menu).
 * Stored as a JSON array in prefs; enabled scripts run after SpotiOS's own
 * injections on every open.spotify.com page load, each in its own try/catch,
 * with a small GM_* compatibility layer.
 */
object UserScripts {

    data class Script(
        val id: String,
        val name: String,
        val code: String,
        val enabled: Boolean = true,
        /** Where it was installed from, for showing and reinstalling. */
        val source: String = "",
        /** The script's @require libraries, fetched at install time, run before it. */
        val lib: String = "",
    )

    /** The parts of a ==UserScript== header the installer shows. */
    data class Meta(
        val name: String?,
        val namespace: String?,
        val version: String?,
        val description: String?,
        val author: String?,
        val matches: List<String>,
        val grants: List<String>,
        val requires: List<String>,
        val runAt: String?,
    )

    private const val PREFS = "spotilol_prefs"
    private const val KEY = "UserScripts"
    const val DEV_MODE_KEY = "DevMode"
    const val REV_KEY = "UserScriptsRev"
    private const val MAX_BYTES = 3 * 1024 * 1024

    /**
     * Developer mode gates user scripts: the Dev menu item, the Settings tiles and
     * running scripts at all. People who added scripts before the switch existed keep them.
     */
    fun devMode(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, 0)
        return if (p.contains(DEV_MODE_KEY)) p.getBoolean(DEV_MODE_KEY, false) else load(context).isNotEmpty()
    }

    fun setDevMode(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(DEV_MODE_KEY, on).apply()
    }

    fun load(context: Context): List<Script> {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let {
                    Script(
                        id = it.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = it.optString("name").ifBlank { "Untitled script" },
                        code = it.optString("code"),
                        enabled = it.optBoolean("enabled", true),
                        source = it.optString("source"),
                        lib = it.optString("lib"),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, scripts: List<Script>) {
        val arr = JSONArray()
        scripts.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("code", it.code)
                put("enabled", it.enabled)
                if (it.source.isNotEmpty()) put("source", it.source)
                if (it.lib.isNotEmpty()) put("lib", it.lib)
            })
        }
        // REV_KEY tells the page to run newly added or enabled scripts right away.
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, arr.toString()).putLong(REV_KEY, System.currentTimeMillis()).apply()
    }

    fun newScript(code: String, name: String? = null): Script =
        Script(UUID.randomUUID().toString(), name?.takeIf { it.isNotBlank() } ?: metaName(code) ?: "Untitled script", code)

    /** "// @name Foo" from a userscript header. */
    fun metaName(code: String): String? =
        Regex("""//\s*@name\s+(.+)""").find(code)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    private fun header(code: String): String? =
        Regex("""==UserScript==([\s\S]*?)==/UserScript==""").find(code)?.groupValues?.get(1)

    fun isUserScript(code: String): Boolean = header(code) != null

    fun meta(code: String): Meta {
        val h = header(code).orEmpty()
        val all = Regex("""//\s*@([\w:.-]+)[ \t]+(.+)""").findAll(h).map { it.groupValues[1] to it.groupValues[2].trim() }.toList()
        fun one(k: String) = all.firstOrNull { it.first == k }?.second
        fun many(vararg k: String) = all.filter { it.first in k }.map { it.second }
        return Meta(
            name = one("name"),
            namespace = one("namespace"),
            version = one("version"),
            description = one("description"),
            author = one("author"),
            matches = many("match", "include"),
            grants = many("grant").filter { it != "none" },
            requires = many("require"),
            runAt = one("run-at"),
        )
    }

    /** The installed script this one would replace: same @name and @namespace. */
    fun existing(context: Context, code: String): Script? {
        val m = meta(code)
        val name = m.name ?: return null
        return load(context).firstOrNull { val o = meta(it.code); o.name == name && o.namespace == m.namespace }
    }

    /** True when the script's @match/@include patterns cover open.spotify.com (or it has none). */
    fun runsOnSpotify(code: String): Boolean = matches(code, "https://open.spotify.com/")

    /** Reads a script from an http(s) link or a content:/file: URI. Call off the main thread. */
    fun fetch(context: Context, source: String): String {
        val uri = Uri.parse(source)
        val bytes = when (uri.scheme?.lowercase()) {
            "content", "file" -> context.contentResolver.openInputStream(uri)?.use { it.readNBytesCompat(MAX_BYTES) }
                ?: error("Couldn't open that file")
            "http", "https" -> {
                val c = URL(source).openConnection() as HttpURLConnection
                c.connectTimeout = 12000
                c.readTimeout = 20000
                c.instanceFollowRedirects = true
                c.setRequestProperty("Accept", "text/javascript, application/javascript, text/plain, */*")
                try {
                    if (c.responseCode !in 200..299) error("The server answered ${c.responseCode}")
                    c.inputStream.use { it.readNBytesCompat(MAX_BYTES) }
                } finally { c.disconnect() }
            }
            else -> error("Unsupported link")
        }
        return bytes.toString(Charsets.UTF_8)
    }

    private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > max) error("That script is too big")
        }
        return out.toByteArray()
    }

    /**
     * Saves [code] (fetching its @require libraries first), replacing an installed copy with
     * the same name. Returns the saved script and whether it was an update. Call off the main thread.
     */
    fun install(context: Context, code: String, source: String): Pair<Script, Boolean> {
        val m = meta(code)
        val lib = buildString {
            m.requires.filter { it.startsWith("http") }.forEach { u ->
                runCatching { fetch(context, u) }.getOrNull()?.let { append(it).append("\n;\n") }
            }
        }
        val list = load(context)
        val old = existing(context, code)
        val script = old?.copy(code = code, name = m.name ?: old.name, source = source, lib = lib)
            ?: Script(UUID.randomUUID().toString(), m.name ?: "Untitled script", code, true, source, lib)
        save(context, if (old != null) list.map { if (it.id == old.id) script else it } else list + script)
        return script to (old != null)
    }

    /** @match / @include patterns; a script with none runs everywhere on Spotify. */
    private fun matches(code: String, url: String): Boolean {
        val header = Regex("""==UserScript==([\s\S]*?)==/UserScript==""").find(code)?.groupValues?.get(1) ?: return true
        val pats = Regex("""//\s*@(?:match|include)\s+(\S+)""").findAll(header).map { it.groupValues[1] }.toList()
        if (pats.isEmpty()) return true
        return pats.any { p ->
            if (p == "*" || p == "<all_urls>") return@any true
            val rx = Regex.escape(p).replace("*", "\\E.*\\Q")
            Regex("^$rx$").matches(url) || Regex("^$rx").containsMatchIn(url)
        }
    }

    private val GM_SHIM = """
        (function(){
          if (window.__spoGM) return; window.__spoGM = 1;
          var P = 'spoGM:';
          window.unsafeWindow = window;
          window.GM_info = { script: { name: 'SpotiOS userscript', version: '1' }, scriptHandler: 'SpotiOS' };
          window.GM_addStyle = function(css){ var s=document.createElement('style'); s.textContent=css; (document.head||document.documentElement).appendChild(s); return s; };
          window.GM_setValue = function(k,v){ try{ localStorage.setItem(P+k, JSON.stringify(v)); }catch(e){} };
          window.GM_getValue = function(k,d){ try{ var v=localStorage.getItem(P+k); return v===null?d:JSON.parse(v); }catch(e){ return d; } };
          window.GM_deleteValue = function(k){ try{ localStorage.removeItem(P+k); }catch(e){} };
          window.GM_listValues = function(){ var o=[]; try{ for(var i=0;i<localStorage.length;i++){ var k=localStorage.key(i); if(k.indexOf(P)===0) o.push(k.slice(P.length)); } }catch(e){} return o; };
          window.GM_log = function(){ try{ console.log.apply(console, arguments); }catch(e){} };
          window.GM_openInTab = function(u){ try{ window.open(u, '_blank'); }catch(e){} };
          window.GM_setClipboard = function(t){ try{ navigator.clipboard.writeText(String(t)); }catch(e){} };
          window.GM_notification = function(d){ try{ var t=typeof d==='string'?d:(d&&d.text); if(window.splToast) splToast(t); }catch(e){} };
          window.GM_registerMenuCommand = function(){ return 0; };
          window.GM_xmlhttpRequest = function(o){
            var ctl = new AbortController();
            fetch(o.url, { method: o.method||'GET', headers: o.headers, body: o.data, signal: ctl.signal })
              .then(function(r){ return r.text().then(function(t){
                var resp = { status: r.status, statusText: r.statusText, responseText: t, response: t, finalUrl: r.url, responseHeaders: '' };
                if (o.responseType === 'json') { try { resp.response = JSON.parse(t); } catch(e){} }
                if (o.onload) o.onload(resp);
              }); })
              .catch(function(e){ if (o.onerror) o.onerror(e); });
            return { abort: function(){ ctl.abort(); } };
          };
          window.GM = {
            getValue: function(k,d){ return Promise.resolve(GM_getValue(k,d)); },
            setValue: function(k,v){ return Promise.resolve(GM_setValue(k,v)); },
            deleteValue: function(k){ return Promise.resolve(GM_deleteValue(k)); },
            listValues: function(){ return Promise.resolve(GM_listValues()); },
            addStyle: function(c){ return Promise.resolve(GM_addStyle(c)); },
            xmlHttpRequest: GM_xmlhttpRequest,
            setClipboard: function(t){ return Promise.resolve(GM_setClipboard(t)); },
            openInTab: GM_openInTab, info: GM_info
          };
        })();
    """.trimIndent()

    /** JS for every enabled script that matches [url], or "" when there are none. */
    fun buildJs(context: Context, url: String): String {
        if (!devMode(context)) return ""
        val active = load(context).filter { it.enabled && it.code.isNotBlank() && matches(it.code, url) }
        if (active.isEmpty()) return ""
        return buildString {
            append(GM_SHIM).append('\n')
            active.forEach {
                append("(function(){ if (window['__spoUS_").append(it.id.replace("-", "")).append("']) return; window['__spoUS_")
                    .append(it.id.replace("-", "")).append("'] = 1;\n")
                append("try{\n")
                if (it.lib.isNotEmpty()) append(it.lib).append("\n")
                append(it.code).append("\n}catch(e){ try{ AndBridge.dbg('e', 'userscript ' + ")
                    .append(JSONObject.quote(it.name)).append(" + ': ' + e); }catch(e2){} }\n")
                append("})();\n")
            }
        }
    }
}
