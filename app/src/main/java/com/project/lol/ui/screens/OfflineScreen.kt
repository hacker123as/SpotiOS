package com.project.lol.ui.screens

import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.project.lol.R
import com.project.lol.offline.CollectionManifest
import com.project.lol.offline.OfflineCollections
import com.project.lol.offline.OfflineSong
import com.project.lol.offline.OfflineStore
import com.project.lol.searchEngine.GenericSearchEngine
import com.project.lol.searchEngine.SearchableFieldExtractor
import com.project.lol.service.OfflineMediaService
import com.project.lol.ui.components.SettingsDialog
import com.project.lol.util.BuildInfo
import compose.icons.TablerIcons
import compose.icons.tablericons.Trash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private const val PREF_OFFLINE_SHUFFLE = "OfflineShuffle"

/** Mutable bits that are not UI state (no recomposition when they change). */
private class OfflineScreenRefs {
    var loadJob: Job? = null
    var buildJob: Job? = null
    var toast: Toast? = null
}

private data class SongMenuTarget(val song: OfflineSong, val sourceKey: String)

/**
 * Offline mode: "Your Library" like the Spotify app when it has no connection. Playlists and
 * albums that were downloaded, each with its full track list (songs that are not on the device
 * are grayed out), plus every downloaded song. Plays only what is already on the device.
 */
@Composable
fun OfflineScreen(
    modifier: Modifier = Modifier,
    prefs: SharedPreferences,
    materialYou: Boolean,
    onMaterialYouChange: (Boolean) -> Unit,
    amoledTheme: Boolean,
    onAmoledThemeChange: (Boolean) -> Unit,
    hideTopBar: Boolean,
    onHideTopBarChange: (Boolean) -> Unit,
    landscapeMode: Boolean,
    onLandscapeModeChange: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    paletteSeed: String?,
    onPaletteSeedChange: (String?) -> Unit,
    onConnectionModeChange: (String) -> Unit,
    onOfflineModeChange: (Boolean) -> Unit,
    onSaveProfile: (String, String) -> Unit,
    onLoadProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onClearCache: () -> Unit,
    onClearData: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val refs = remember { OfflineScreenRefs() }

    var settingsDialogOpen by remember { mutableStateOf(false) }
    var settingsWasOpen by remember { mutableStateOf(false) }

    // Library
    var songs by remember { mutableStateOf<List<OfflineSong>>(emptyList()) }
    var manifests by remember { mutableStateOf<List<CollectionManifest>>(emptyList()) }
    var collectionCovers by remember { mutableStateOf<Map<String, File>>(emptyMap()) }
    var library by remember { mutableStateOf<OfflineLibrary?>(null) }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf<LibraryFilter?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var songResults by remember { mutableStateOf<List<OfflineSong>?>(null) }
    val libraryListState = rememberLazyListState()

    // Menus and confirmations
    var songMenu by remember { mutableStateOf<SongMenuTarget?>(null) }
    var itemMenu by remember { mutableStateOf<LibraryItem?>(null) }
    var pendingDelete by remember { mutableStateOf<OfflineSong?>(null) }
    var pendingRemove by remember { mutableStateOf<LibraryItem?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    // Playback
    val mediaPlayer = remember { MediaPlayer() }
    var queue by remember { mutableStateOf<List<OfflineSong>>(emptyList()) }
    var baseQueue by remember { mutableStateOf<List<OfflineSong>>(emptyList()) }
    var queueIndex by remember { mutableIntStateOf(-1) }
    var queueSource by remember { mutableStateOf<String?>(null) }
    var playerSong by remember { mutableStateOf<OfflineSong?>(null) }
    val lastPlayerSong = remember { mutableStateOf<OfflineSong?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var scrubMs by remember { mutableIntStateOf(-1) }
    var shuffleOn by remember { mutableStateOf(prefs.getBoolean(PREF_OFFLINE_SHUFFLE, false)) }
    var nowPlayingOpen by remember { mutableStateOf(false) }

    val searchEngine = remember { GenericSearchEngine<OfflineSong>(maxResult = 100) }
    val songExtractor = remember {
        SearchableFieldExtractor<OfflineSong> { song ->
            arrayOf(song.title, song.artist, song.album, song.ytAlbum, song.ytArtist)
        }
    }

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: ""
    }
    val versionLabel = stringResource(R.string.offline_app_name) + " " +
        stringResource(R.string.offline_version, versionName, BuildInfo.id)
    val notDownloadedText = stringResource(R.string.offline_lib_not_downloaded_toast)
    val nothingToPlayText = stringResource(R.string.offline_lib_nothing_to_play)
    val couldNotPlayText = stringResource(R.string.offline_lib_couldnt_play)

    fun showToast(text: String) {
        refs.toast?.cancel()
        refs.toast = Toast.makeText(context, text, Toast.LENGTH_SHORT).also { it.show() }
    }

    fun syncService() {
        val song = playerSong ?: return
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, OfflineMediaService::class.java).apply {
                    putExtra("title", song.title)
                    putExtra("artist", song.artist)
                    putExtra("album", song.album)
                    putExtra("duration", durationMs.toLong())
                    putExtra("playing", isPlaying)
                    putExtra("position", positionMs.toLong())
                    putExtra("coverPath", song.coverFile?.absolutePath)
                }
            )
        }
    }

    fun play(index: Int) {
        val song = queue.getOrNull(index) ?: return
        val ok = runCatching {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, song.uri)
            mediaPlayer.prepare()
            mediaPlayer.start()
        }.isSuccess
        queueIndex = index
        playerSong = song
        positionMs = 0
        if (ok) {
            isPlaying = true
            durationMs = runCatching { mediaPlayer.duration }.getOrDefault(0).coerceAtLeast(0)
        } else {
            isPlaying = false
            durationMs = 0
            showToast(couldNotPlayText)
        }
        syncService()
    }

    fun togglePlayPause() {
        runCatching {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                isPlaying = false
            } else if (durationMs > 0) {
                mediaPlayer.start()
                isPlaying = true
            } else if (playerSong != null && queueIndex >= 0) {
                play(queueIndex)
                return
            }
        }
        syncService()
    }

    fun step(delta: Int) {
        if (queue.isEmpty()) return
        val next = ((queueIndex + delta) % queue.size + queue.size) % queue.size
        play(next)
    }

    fun seekTo(position: Long) {
        if (durationMs <= 0) return
        runCatching { mediaPlayer.seekTo(position.toInt()) }
        positionMs = position.toInt()
        OfflineMediaService.instance?.updatePosition(position)
    }

    fun previous() {
        // Like Spotify: a few seconds in, "previous" restarts the song.
        if (durationMs > 0 && positionMs > 3000) seekTo(0) else step(-1)
    }

    fun stopAndClear() {
        runCatching {
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
            mediaPlayer.reset()
        }
        isPlaying = false
        queue = emptyList()
        baseQueue = emptyList()
        queueIndex = -1
        queueSource = null
        playerSong = null
        positionMs = 0
        durationMs = 0
        nowPlayingOpen = false
        runCatching { context.stopService(Intent(context, OfflineMediaService::class.java)) }
    }

    /** Starts [item]'s downloaded songs, from [startId] if given. Next and previous stay inside it. */
    fun startQueue(item: LibraryItem, startId: String?, shuffle: Boolean) {
        val list = item.songs
        if (list.isEmpty()) {
            showToast(nothingToPlayText)
            return
        }
        val start = startId?.let { id -> list.indexOfFirst { it.id == id } }?.takeIf { it >= 0 }
        baseQueue = list
        queueSource = item.key
        if (shuffle) {
            val first = list[start ?: list.indices.random()]
            queue = listOf(first) + list.filter { it.id != first.id }.shuffled()
            play(0)
        } else {
            queue = list
            play(start ?: 0)
        }
    }

    fun setShuffle(on: Boolean) {
        shuffleOn = on
        prefs.edit().putBoolean(PREF_OFFLINE_SHUFFLE, on).apply()
        val current = queue.getOrNull(queueIndex) ?: return
        if (on) {
            queue = listOf(current) + baseQueue.filter { it.id != current.id }.shuffled()
            queueIndex = 0
        } else {
            queue = baseQueue
            queueIndex = baseQueue.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        }
    }

    fun removeFromQueue(ids: Set<String>) {
        if (ids.isEmpty() || queue.isEmpty()) return
        val current = playerSong
        if (current != null && current.id in ids) {
            stopAndClear()
            return
        }
        baseQueue = baseQueue.filterNot { it.id in ids }
        queue = queue.filterNot { it.id in ids }
        queueIndex = queue.indexOfFirst { it.id == current?.id }
    }

    fun rebuild(newSongs: List<OfflineSong>) {
        songs = newSongs
        val m = manifests
        val c = collectionCovers
        refs.buildJob?.cancel()
        refs.buildJob = scope.launch {
            library = withContext(Dispatchers.Default) { buildOfflineLibrary(newSongs, m, c) }
        }
    }

    fun reload() {
        refs.loadJob?.cancel()
        refs.buildJob?.cancel()
        refs.loadJob = scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val s = runCatching { OfflineStore.loadSongs(context) }.getOrDefault(emptyList())
                val m = runCatching { OfflineCollections.loadAll(context) }.getOrDefault(emptyList())
                val c = m.mapNotNull { mf -> OfflineCollections.coverFile(context, mf.name)?.let { mf.name to it } }.toMap()
                Triple(s, m, c)
            }
            val built = withContext(Dispatchers.Default) {
                buildOfflineLibrary(loaded.first, loaded.second, loaded.third)
            }
            songs = loaded.first
            manifests = loaded.second
            collectionCovers = loaded.third
            library = built
            val ids = loaded.first.mapTo(HashSet()) { it.id }
            removeFromQueue(queue.mapNotNull { it.id.takeIf { id -> id !in ids } }.toSet())
        }
    }

    fun performDelete(song: OfflineSong) {
        removeFromQueue(setOf(song.id))
        rebuild(songs.filterNot { it.id == song.id && it.uri == song.uri })
        scope.launch {
            val ok = withContext(Dispatchers.IO) { OfflineStore.deleteSong(context, song) }
            if (!ok) {
                showToast(context.getString(R.string.offline_toast_could_not_delete_file))
                reload()
            }
        }
    }

    fun performRemove(item: LibraryItem) {
        val targets = item.owned
        removeFromQueue(targets.mapTo(HashSet()) { it.id })
        if (openKey == item.key) openKey = null
        if (item.kind == LibraryKind.Downloads) {
            searchQuery = ""
            songResults = null
        }
        scope.launch {
            val failed = withContext(Dispatchers.IO) {
                val f = targets.count { song -> !OfflineStore.deleteSong(context, song) }
                when {
                    item.kind == LibraryKind.Downloads -> OfflineCollections.deleteAll(context)
                    item.isCollection -> OfflineCollections.delete(context, item.name)
                }
                f
            }
            if (failed > 0) {
                showToast(
                    if (failed == 1) {
                        context.getString(R.string.offline_toast_could_not_delete_one_file)
                    } else {
                        context.getString(R.string.offline_toast_could_not_delete_files, failed)
                    }
                )
            }
            reload()
        }
    }

    fun onPlayItem(item: LibraryItem) {
        if (queueSource == item.key && playerSong != null) togglePlayPause() else startQueue(item, null, shuffleOn)
    }

    fun onShuffleItem(item: LibraryItem) {
        if (queueSource == item.key && playerSong != null) {
            setShuffle(!shuffleOn)
        } else {
            shuffleOn = true
            prefs.edit().putBoolean(PREF_OFFLINE_SHUFFLE, true).apply()
            startQueue(item, null, true)
        }
    }

    fun onSongTap(item: LibraryItem?, song: OfflineSong) {
        if (item == null) return
        if (playerSong?.id == song.id && queueSource == item.key) {
            togglePlayPause()
        } else {
            startQueue(item, song.id, shuffleOn)
        }
    }

    fun openItem(item: LibraryItem) {
        focusManager.clearFocus()
        keyboardController?.hide()
        openKey = item.key
    }

    BackHandler(enabled = settingsDialogOpen || nowPlayingOpen || openKey != null || searchFocused || searchOpen) {
        when {
            settingsDialogOpen -> settingsDialogOpen = false
            nowPlayingOpen -> nowPlayingOpen = false
            openKey != null -> openKey = null
            searchFocused -> {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
            else -> {
                searchOpen = false
                searchQuery = ""
            }
        }
    }

    DisposableEffect(Unit) {
        val ctrl = object : OfflineMediaService.OfflineController {
            override fun onPlayPause() = togglePlayPause()
            override fun onNext() = step(1)
            override fun onPrev() = previous()
            override fun onStop() = stopAndClear()
            override fun onSeekTo(position: Long) = seekTo(position)
        }
        OfflineMediaService.controller = ctrl
        onDispose {
            if (OfflineMediaService.controller === ctrl) OfflineMediaService.controller = null
            runCatching { mediaPlayer.release() }
            runCatching { context.stopService(Intent(context, OfflineMediaService::class.java)) }
        }
    }

    DisposableEffect(mediaPlayer) {
        mediaPlayer.setOnCompletionListener {
            if (queueIndex in 0 until queue.lastIndex) {
                play(queueIndex + 1)
            } else {
                isPlaying = false
                positionMs = 0
                OfflineMediaService.instance?.updatePlaying(false, 0)
            }
        }
        onDispose { }
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(settingsDialogOpen) {
        // Songs or playlists may have been removed from Settings > Downloads.
        if (settingsDialogOpen) {
            settingsWasOpen = true
        } else if (settingsWasOpen) {
            settingsWasOpen = false
            reload()
        }
    }

    LaunchedEffect(library, openKey) {
        val lib = library ?: return@LaunchedEffect
        if (openKey != null && lib.item(openKey) == null) openKey = null
    }

    LaunchedEffect(playerSong) {
        playerSong?.let { lastPlayerSong.value = it }
    }

    LaunchedEffect(searchQuery, songs) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            songResults = null
        } else {
            delay(250.milliseconds)
            songResults = withContext(Dispatchers.Default) { searchEngine.filter(songs, q, songExtractor) }
        }
    }

    LaunchedEffect(isPlaying, queueIndex) {
        while (isPlaying) {
            runCatching { positionMs = mediaPlayer.currentPosition }
            OfflineMediaService.instance?.updatePosition(positionMs.toLong())
            delay(500.milliseconds)
        }
    }

    SettingsDialog(
        visible = settingsDialogOpen,
        onClose = { settingsDialogOpen = false },
        prefs = prefs,
        materialYou = materialYou,
        onMaterialYouChange = onMaterialYouChange,
        amoledThemeState = amoledTheme,
        onAmoledThemeChange = onAmoledThemeChange,
        hideTopBar = hideTopBar,
        onHideTopBarChange = onHideTopBarChange,
        landscapeMode = landscapeMode,
        onLandscapeModeChange = onLandscapeModeChange,
        keepScreenOn = keepScreenOn,
        onKeepScreenOnChange = onKeepScreenOnChange,
        paletteSeed = paletteSeed,
        onPaletteSeedChange = onPaletteSeedChange,
        onConnectionModeChange = onConnectionModeChange,
        onOfflineModeChange = onOfflineModeChange,
        onSaveProfile = onSaveProfile,
        onLoadProfile = onLoadProfile,
        onDeleteProfile = onDeleteProfile,
        onClearCache = onClearCache,
        onClearData = onClearData,
        onDebugToggle = {},
        blockServiceWorker = prefs.getBoolean("BlockServiceWorker", true),
        onBlockServiceWorkerChange = { enabled ->
            prefs.edit().putBoolean("BlockServiceWorker", enabled).apply()
        }
    ) {
        val background = if (amoledTheme) Color.Black else OfflinePalette.Background
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val listBottom = navBottom + if (playerSong != null) 88.dp else 24.dp

        ProvideTextStyle(
            MaterialTheme.typography.bodyMedium.copy(
                color = OfflinePalette.TextPrimary,
                lineHeight = TextUnit.Unspecified,
                fontFeatureSettings = "lnum"
            )
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(background)
            ) {
                AnimatedContent(
                    targetState = openKey,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(240)))
                                .togetherWith(slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 8 } + fadeOut(tween(200)))
                        } else {
                            (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 8 } + fadeIn(tween(240)))
                                .togetherWith(slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 3 } + fadeOut(tween(200)))
                        }.using(SizeTransform(clip = false))
                    },
                    label = "offlineNav"
                ) { key ->
                    if (key == null) {
                        LibraryPage(
                            library = library,
                            listState = libraryListState,
                            filter = filter,
                            onFilterChange = { f ->
                                filter = f
                                scope.launch { runCatching { libraryListState.scrollToItem(0) } }
                            },
                            searchOpen = searchOpen,
                            onSearchOpenChange = { open ->
                                searchOpen = open
                                if (!open) {
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearchFocusChange = { searchFocused = it },
                            songResults = songResults,
                            currentSongId = playerSong?.id,
                            isPlaying = isPlaying,
                            playingSourceKey = queueSource,
                            bottomPadding = listBottom,
                            versionLabel = versionLabel,
                            onOpenItem = { openItem(it) },
                            onItemMenu = { itemMenu = it },
                            onPlaySong = { song -> onSongTap(library?.item(DOWNLOADS_KEY), song) },
                            onSongMenu = { song -> songMenu = SongMenuTarget(song, DOWNLOADS_KEY) },
                            onSettings = { settingsDialogOpen = true },
                            onGoOnline = onExit
                        )
                    } else {
                        val item = library?.item(key)
                        if (item == null) {
                            Box(Modifier.fillMaxSize())
                        } else {
                            CollectionPage(
                                item = item,
                                background = background,
                                currentSongId = playerSong?.id,
                                isPlaying = isPlaying,
                                isCurrentSource = queueSource == item.key && playerSong != null,
                                shuffleOn = shuffleOn,
                                bottomPadding = listBottom,
                                onBack = { openKey = null },
                                onPlay = { onPlayItem(item) },
                                onShuffle = { onShuffleItem(item) },
                                onMore = { itemMenu = item },
                                onTrackClick = { track ->
                                    val song = track.song
                                    if (song == null) showToast(notDownloadedText) else onSongTap(item, song)
                                },
                                onTrackMenu = { song -> songMenu = SongMenuTarget(song, item.key) }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = playerSong != null && !nowPlayingOpen,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160))
                ) {
                    val song = playerSong ?: lastPlayerSong.value
                    if (song != null) {
                        MiniPlayer(
                            song = song,
                            isPlaying = isPlaying,
                            progress = {
                                val shown = if (scrubMs >= 0) scrubMs else positionMs
                                if (durationMs > 0) shown.toFloat() / durationMs else 0f
                            },
                            onOpen = { nowPlayingOpen = true },
                            onTogglePlay = { togglePlayPause() },
                            onNext = { step(1) },
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(bottom = 8.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = nowPlayingOpen && playerSong != null,
                    enter = slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(240))
                ) {
                    val song = playerSong ?: lastPlayerSong.value
                    if (song != null) {
                        val source = library?.item(queueSource)
                        NowPlayingView(
                            song = song,
                            sourceLabel = when (source?.kind) {
                                LibraryKind.Album -> stringResource(R.string.offline_lib_playing_from_album)
                                LibraryKind.Playlist, LibraryKind.Liked -> stringResource(R.string.offline_lib_playing_from_playlist)
                                else -> stringResource(R.string.offline_lib_playing_from_library)
                            },
                            sourceName = source?.displayName() ?: stringResource(R.string.offline_lib_downloaded_songs),
                            isPlaying = isPlaying,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            scrubMs = scrubMs,
                            shuffleOn = shuffleOn,
                            background = background,
                            onScrub = { scrubMs = it },
                            onScrubFinished = {
                                if (scrubMs >= 0) seekTo(scrubMs.toLong())
                                scrubMs = -1
                            },
                            onTogglePlay = { togglePlayPause() },
                            onPrev = { previous() },
                            onNext = { step(1) },
                            onShuffle = { setShuffle(!shuffleOn) },
                            onClose = { nowPlayingOpen = false },
                            onStop = { stopAndClear() }
                        )
                    }
                }
            }

            songMenu?.let { target ->
                val song = target.song
                val unknownArtist = stringResource(R.string.offline_unknown_artist)
                OptionsSheet(
                    title = song.title,
                    subtitle = song.artist.ifBlank { unknownArtist },
                    artwork = { CoverImage(art = song.coverArt(), size = 48.dp, corner = 4.dp) },
                    actions = listOf(
                        SheetAction(OfflineIcons.Play, stringResource(R.string.offline_lib_play)) {
                            val source = library?.item(target.sourceKey) ?: library?.item(DOWNLOADS_KEY)
                            if (source != null) startQueue(source, song.id, shuffleOn)
                        },
                        SheetAction(TablerIcons.Trash, stringResource(R.string.offline_lib_remove_download)) {
                            pendingDelete = song
                        }
                    ),
                    onDismiss = { songMenu = null }
                )
            }

            itemMenu?.let { item ->
                val actions = buildList {
                    if (item.downloaded > 0) {
                        add(SheetAction(OfflineIcons.Play, stringResource(R.string.offline_lib_play)) { startQueue(item, null, false) })
                        add(
                            SheetAction(OfflineIcons.Shuffle, stringResource(R.string.offline_lib_shuffle_play)) {
                                shuffleOn = true
                                prefs.edit().putBoolean(PREF_OFFLINE_SHUFFLE, true).apply()
                                startQueue(item, null, true)
                            }
                        )
                    }
                    if (item.kind == LibraryKind.Downloads) {
                        add(SheetAction(TablerIcons.Trash, stringResource(R.string.offline_lib_remove_all_downloads)) { confirmDeleteAll = true })
                    } else {
                        add(SheetAction(TablerIcons.Trash, stringResource(R.string.offline_lib_remove_download)) { pendingRemove = item })
                    }
                }
                OptionsSheet(
                    title = item.displayName(),
                    subtitle = item.kindLabel() + " · " + pluralStringResource(R.plurals.offline_lib_songs, item.total, item.total),
                    artwork = { CollectionArtwork(item = item, size = 48.dp, corner = 4.dp) },
                    actions = actions,
                    onDismiss = { itemMenu = null }
                )
            }
        }
    }

    val songToDelete = pendingDelete
    if (songToDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.offline_dialog_delete_song_title),
            message = stringResource(
                R.string.offline_dialog_delete_song_message,
                songToDelete.title,
                songToDelete.artist.ifBlank { stringResource(R.string.offline_unknown_artist) }
            ),
            confirmLabel = stringResource(R.string.offline_dialog_delete),
            onConfirm = {
                pendingDelete = null
                performDelete(songToDelete)
            },
            onDismiss = { pendingDelete = null }
        )
    }

    val itemToRemove = pendingRemove
    if (itemToRemove != null) {
        ConfirmDialog(
            title = stringResource(R.string.offline_lib_remove_title),
            message = stringResource(R.string.offline_lib_remove_message, itemToRemove.displayName()),
            confirmLabel = stringResource(R.string.offline_lib_remove),
            onConfirm = {
                pendingRemove = null
                performRemove(itemToRemove)
            },
            onDismiss = { pendingRemove = null }
        )
    }

    if (confirmDeleteAll) {
        val total = songs.size
        ConfirmDialog(
            title = stringResource(R.string.offline_dialog_delete_all_title),
            message = if (total == 1) {
                stringResource(R.string.offline_dialog_delete_all_message_one)
            } else {
                stringResource(R.string.offline_dialog_delete_all_message_many, total)
            },
            confirmLabel = stringResource(R.string.offline_dialog_delete_all),
            onConfirm = {
                confirmDeleteAll = false
                library?.item(DOWNLOADS_KEY)?.let { performRemove(it) }
            },
            onDismiss = { confirmDeleteAll = false }
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = OfflinePalette.Sheet,
        titleContentColor = OfflinePalette.TextPrimary,
        textContentColor = OfflinePalette.TextSecondary,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.offline_dialog_cancel), color = OfflinePalette.TextSecondary)
            }
        }
    )
}
