package com.project.lol.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.lol.offline.OfflineSong
import com.project.lol.offline.OfflineStore
import compose.icons.TablerIcons
import compose.icons.tablericons.Download
import compose.icons.tablericons.Trash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lists downloaded music grouped by the playlist or album it came with, so a
 * whole playlist (or everything) can be removed from Settings.
 */
@Composable
fun DownloadsManagerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var songs by remember { mutableStateOf<List<OfflineSong>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        songs = withContext(Dispatchers.IO) { runCatching { OfflineStore.loadSongs(context) }.getOrDefault(emptyList()) }
    }

    fun delete(list: List<OfflineSong>) {
        if (list.isEmpty() || busy) return
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) { list.forEach { runCatching { OfflineStore.deleteSong(context, it) } } }
            val ids = list.map { it.id to it.uri }.toSet()
            songs = songs?.filterNot { (it.id to it.uri) in ids }
            busy = false
        }
    }

    val groups = remember(songs) {
        songs.orEmpty()
            .groupBy { it.collection.ifBlank { it.album.ifBlank { "Single songs" } } }
            .toList()
            .sortedByDescending { it.second.size }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(TablerIcons.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Downloaded music", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    songs?.let {
                        Text(
                            "${it.size} songs · ${groups.size} groups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Box(Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 420.dp)) {
                when {
                    songs == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    groups.isEmpty() -> Text(
                        "Nothing downloaded yet. Use the download buttons on songs, albums and playlists.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(groups, key = { it.first }) { (name, list) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (list.size == 1) "1 song" else "${list.size} songs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { delete(list) }, enabled = !busy) {
                                    Icon(TablerIcons.Trash, contentDescription = "Remove $name", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            if (!songs.isNullOrEmpty()) {
                TextButton(
                    enabled = !busy,
                    onClick = { if (confirmAll) { confirmAll = false; delete(songs.orEmpty()) } else confirmAll = true }
                ) {
                    Text(
                        if (confirmAll) "Tap again to remove all" else "Remove all",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}
