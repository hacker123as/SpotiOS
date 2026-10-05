package com.project.lol.webview.helpers

import android.content.Context
import com.project.lol.util.Logger
import org.json.JSONObject

/**
 * SpotiOS - LilAmi Liquid OS 26 extra glass layer.
 *
 * Optional extra glass on top of the SpotiOS shell (SpotiOSUi), off by
 * default. Its wallpaper and player sections only apply without the shell. The stylesheet lives in
 * assets/themes/liquid_glass.css and is injected as
 * <style id="spotilol-liquid-glass">, placed before 'spotilol-custom-css'
 * so the user's Custom CSS can still override any of its tokens.
 */
object LiquidGlassTheme {

    const val PREF_KEY = "LiquidGlassTheme"
    const val DEFAULT_ENABLED = false

    private const val TAG = "LiquidGlassTheme"
    private const val ASSET_PATH = "themes/liquid_glass.css"
    private const val STYLE_ID = "spotilol-liquid-glass"
    private const val CUSTOM_CSS_ID = "spotilol-custom-css"

    // The asset never changes at runtime; quote it for JS once.
    @Volatile private var quotedCss: String? = null

    // Retired in 2.4.0: layered over the SpotiOS shell it glitched, so it stays
    // off and buildJs() only removes a style left over from older versions.
    @Suppress("UNUSED_PARAMETER")
    fun isEnabled(context: Context): Boolean = false

    private fun quotedCss(context: Context): String? {
        quotedCss?.let { return it }
        return runCatching {
            context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        }.onFailure {
            Logger.e(TAG, "failed to read $ASSET_PATH: ${it.message}")
        }.getOrNull()?.let { JSONObject.quote(it) }?.also { quotedCss = it }
    }

    fun buildJs(context: Context): String = buildJs(context, isEnabled(context))

    fun buildJs(context: Context, enabled: Boolean): String {
        val css = if (enabled) quotedCss(context) else null
        return if (css == null) {
            """
            (function(){
                var st = document.getElementById('$STYLE_ID');
                if (st) st.remove();
            })();
            """.trimIndent()
        } else {
            """
            (function(){
                var css = $css;
                var st = document.getElementById('$STYLE_ID');
                if (!st) {
                    st = document.createElement('style');
                    st.id = '$STYLE_ID';
                }
                if (st.textContent !== css) st.textContent = css;
                if (!st.parentNode) {
                    var target = document.head || document.documentElement;
                    var custom = document.getElementById('$CUSTOM_CSS_ID');
                    if (custom && custom.parentNode) custom.parentNode.insertBefore(st, custom);
                    else if (target) target.appendChild(st);
                }
            })();
            """.trimIndent()
        }
    }
}
