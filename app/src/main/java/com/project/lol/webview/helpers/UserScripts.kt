package com.project.lol.webview.helpers

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
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
    )

    private const val PREFS = "spotilol_prefs"
    private const val KEY = "UserScripts"

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
            })
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, arr.toString()).apply()
    }

    fun newScript(code: String, name: String? = null): Script =
        Script(UUID.randomUUID().toString(), name?.takeIf { it.isNotBlank() } ?: metaName(code) ?: "Untitled script", code)

    /** "// @name Foo" from a userscript header. */
    fun metaName(code: String): String? =
        Regex("""//\s*@name\s+(.+)""").find(code)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

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
        val active = load(context).filter { it.enabled && it.code.isNotBlank() && matches(it.code, url) }
        if (active.isEmpty()) return ""
        return buildString {
            append(GM_SHIM).append('\n')
            active.forEach {
                append("(function(){ if (window['__spoUS_").append(it.id.replace("-", "")).append("']) return; window['__spoUS_")
                    .append(it.id.replace("-", "")).append("'] = 1;\n")
                append("try{\n").append(it.code).append("\n}catch(e){ try{ AndBridge.dbg('e', 'userscript ' + ")
                    .append(JSONObject.quote(it.name)).append(" + ': ' + e); }catch(e2){} }\n")
                append("})();\n")
            }
        }
    }
}
