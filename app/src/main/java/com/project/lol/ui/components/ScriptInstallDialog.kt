package com.project.lol.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.project.lol.webview.helpers.UserScripts
import compose.icons.TablerIcons
import compose.icons.tablericons.AlertTriangle
import compose.icons.tablericons.Code
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tampermonkey-style installer for a user script opened from a .user.js link, a Greasy Fork
 * page shared to SpotiOS, a .js file or Settings > Dev > Install from link. Asks to turn on
 * Developer mode first when it's off. [onClose] gets true after a script was installed, so
 * the page can reload and run it.
 */
@Composable
fun ScriptInstallDialog(source: String, onClose: (installed: Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var devOn by remember { mutableStateOf(UserScripts.devMode(context)) }
    var attempt by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf(false) }
    var showSource by remember { mutableStateOf(false) }

    LaunchedEffect(devOn, attempt) {
        if (!devOn) return@LaunchedEffect
        code = null; error = null
        val result = withContext(Dispatchers.IO) { runCatching { UserScripts.fetch(context, source) } }
        result.onSuccess { body ->
            if (UserScripts.isUserScript(body)) code = body
            else error = "This isn't a user script. It has no ==UserScript== header."
        }.onFailure { error = it.message ?: "Couldn't download the script" }
    }

    Dialog(onDismissRequest = { if (!installing) onClose(false) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 640.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(TablerIcons.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Install user script", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(hostOf(source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(16.dp))

                val c = code
                when {
                    !devOn -> {
                        Text("Developer mode is off", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "User scripts are a developer feature. Turn on Developer mode to install this script. You can turn it off again in Settings > Advanced > Dev.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onClose(false) }) { Text("Cancel") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { UserScripts.setDevMode(context, true); devOn = true }) { Text("Turn on and continue") }
                        }
                    }
                    error != null -> {
                        Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onClose(false) }) { Text("Close") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { attempt++ }) { Text("Try again") }
                        }
                    }
                    c == null -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 18.dp)) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Spacer(Modifier.width(14.dp))
                            Text("Getting the script…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {
                        val meta = remember(c) { UserScripts.meta(c) }
                        val old = remember(c) { UserScripts.existing(context, c) }
                        val onSpotify = remember(c) { UserScripts.runsOnSpotify(c) }
                        val oldVersion = remember(old) { old?.let { UserScripts.meta(it.code).version } }
                        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                            Text(meta.name ?: "Untitled script", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            val line = listOfNotNull(meta.version?.let { "Version $it" }, meta.author?.let { "by $it" }).joinToString("  ·  ")
                            if (line.isNotEmpty()) Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            meta.description?.let {
                                Spacer(Modifier.height(10.dp))
                                Text(it, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(16.dp))
                            Detail("Runs on", if (meta.matches.isEmpty()) "Every Spotify page" else meta.matches.joinToString("\n"))
                            if (meta.grants.isNotEmpty()) Detail("Asks for", meta.grants.joinToString(", "))
                            if (meta.requires.isNotEmpty()) Detail("Also loads", meta.requires.joinToString("\n") { hostOf(it) + " " + it.substringAfterLast('/') })
                            Detail("Size", "${c.lines().size} lines, ${"%.1f".format(c.length / 1024f)} KB")
                            if (old != null) Detail("Installed", "Version ${oldVersion ?: "unknown"}. Installing replaces it and keeps it on or off as it was.")
                            if (!onSpotify) Warning("This script isn't made for open.spotify.com, so it may do nothing here.")
                            if (meta.runAt == "document-start") Warning("This script asks to run before the page loads. SpotiOS runs scripts once Spotify has loaded, so parts of it may not work.")
                            Warning("Scripts can read and change everything on the Spotify page, including your account. Only install scripts you trust.")
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (showSource) "Hide code" else "Show code",
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showSource = !showSource }.padding(vertical = 6.dp)
                            )
                            AnimatedVisibility(showSource) {
                                Box(
                                    Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.35f)).padding(10.dp)
                                        .verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())
                                ) {
                                    Text(c.lines().take(400).joinToString("\n"), fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            if (installing) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
                                Spacer(Modifier.width(12.dp))
                            }
                            TextButton(enabled = !installing, onClick = { onClose(false) }) { Text("Cancel") }
                            Spacer(Modifier.width(8.dp))
                            Button(enabled = !installing, onClick = {
                                installing = true
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) { runCatching { UserScripts.install(context, c, source) } }
                                    installing = false
                                    ok.onSuccess { (sc, updated) ->
                                        android.widget.Toast.makeText(context, (if (updated) "Updated " else "Installed ") + sc.name, android.widget.Toast.LENGTH_SHORT).show()
                                        onClose(true)
                                    }.onFailure { error = it.message ?: "Couldn't install the script" }
                                }
                            }) { Text(if (old != null) "Update" else "Install") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Warning(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)).padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(TablerIcons.AlertTriangle, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

private fun hostOf(source: String): String {
    val u = runCatching { Uri.parse(source) }.getOrNull() ?: return source
    return when (u.scheme?.lowercase()) {
        "content", "file" -> "From a file"
        else -> u.host ?: source
    }
}
