package com.project.lol.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lol.webview.helpers.UserScripts
import compose.icons.TablerIcons
import compose.icons.tablericons.Code
import compose.icons.tablericons.Edit
import compose.icons.tablericons.Trash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Dev > user scripts: add (paste or install from a .user.js URL), edit,
 * enable/disable and delete Tampermonkey-style scripts for the Spotify page.
 * [onClose] gets true when something changed, so the page can reload.
 */
@Composable
fun DevScriptsDialog(onClose: (changed: Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scripts by remember { mutableStateOf(UserScripts.load(context)) }
    var changed by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<UserScripts.Script?>(null) }
    var adding by remember { mutableStateOf(false) }

    fun persist(list: List<UserScripts.Script>) {
        scripts = list
        UserScripts.save(context, list)
        changed = true
    }

    if (adding || editing != null) {
        val original = editing
        var name by remember(original) { mutableStateOf(original?.name ?: "") }
        var code by remember(original) { mutableStateOf(original?.code ?: "") }
        var url by remember { mutableStateOf("") }
        var status by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { adding = false; editing = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (original == null) "New script" else "Edit script", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (original == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = url, onValueChange = { url = it },
                                label = { Text("Install from URL (.user.js)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                val target = url.trim()
                                if (!target.startsWith("http")) { status = "Enter an http(s) link"; return@TextButton }
                                status = "Downloading…"
                                scope.launch {
                                    val body = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val c = URL(target).openConnection() as HttpURLConnection
                                            c.connectTimeout = 10000; c.readTimeout = 15000
                                            c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                                        }.getOrNull()
                                    }
                                    if (body.isNullOrBlank()) status = "Couldn't download that script"
                                    else { code = body; if (name.isBlank()) name = UserScripts.metaName(body) ?: ""; status = "Loaded, tap Save" }
                                }
                            }) { Text("Get") }
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = code, onValueChange = { code = it },
                        label = { Text("Script (paste a Tampermonkey script)") },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp)
                    )
                    status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(
                        "Scripts run on open.spotify.com after SpotiOS loads. GM_addStyle, GM_getValue/GM_setValue, GM_xmlhttpRequest and GM.* are available. Only add scripts you trust.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = code.isNotBlank(), onClick = {
                    val list = if (original == null) scripts + UserScripts.newScript(code, name)
                    else scripts.map { if (it.id == original.id) it.copy(name = name.ifBlank { UserScripts.metaName(code) ?: it.name }, code = code) else it }
                    persist(list)
                    adding = false; editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { adding = false; editing = null }) { Text("Cancel") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = { onClose(changed) },
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(TablerIcons.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Dev · User scripts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Like Tampermonkey, for the Spotify page", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Box(Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 420.dp)) {
                if (scripts.isEmpty()) {
                    Text("No scripts yet. Tap Add to paste one or install it from a link.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else LazyColumn {
                    items(scripts, key = { it.id }) { sc ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(sc.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                Text("${sc.code.lines().size} lines", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = sc.enabled, onCheckedChange = { on -> persist(scripts.map { if (it.id == sc.id) it.copy(enabled = on) else it }) })
                            IconButton(onClick = { editing = sc }) { Icon(TablerIcons.Edit, contentDescription = "Edit ${sc.name}") }
                            IconButton(onClick = { persist(scripts.filterNot { it.id == sc.id }) }) {
                                Icon(TablerIcons.Trash, contentDescription = "Delete ${sc.name}", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        },
        confirmButton = { TextButton(onClick = { onClose(changed) }) { Text(if (changed) "Done, reload page" else "Done") } },
        dismissButton = { TextButton(onClick = { adding = true }) { Text("Add") } }
    )
}
