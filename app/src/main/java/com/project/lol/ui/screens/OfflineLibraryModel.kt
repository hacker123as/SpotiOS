package com.project.lol.ui.screens

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.project.lol.offline.CollectionManifest
import com.project.lol.offline.OfflineSong
import java.io.File

internal const val DOWNLOADS_KEY = "@downloads"
internal const val SINGLES_KEY = "@singles"

internal enum class LibraryKind { Downloads, Liked, Playlist, Album, Singles }

internal enum class LibraryFilter { Playlists, Albums, Songs }

/** Artwork source: a cover file, else the picture embedded in the audio file. */
@Immutable
internal data class CoverArt(val file: File?, val audio: Uri? = null) {
    val cacheKey: String = file?.absolutePath ?: audio?.toString().orEmpty()
}

internal fun OfflineSong.coverArt(): CoverArt = CoverArt(coverFile, uri)

/** One row of a playlist or album. [song] is null when it is not on the device. */
@Immutable
internal data class LibraryTrack(
    val key: String,
    val title: String,
    val artist: String,
    val album: String,
    val song: OfflineSong?,
    val art: CoverArt?,
)

@Immutable
internal class LibraryItem(
    val key: String,
    /** Playlist or album name; blank for the built-in rows, which take their name from resources. */
    val name: String,
    val kind: LibraryKind,
    val tracks: List<LibraryTrack>,
    /** Songs filed under this group (same grouping as the downloads manager); removed with it. */
    val owned: List<OfflineSong>,
    val cover: CoverArt?,
    /** Four covers for a playlist without its own artwork, empty otherwise. */
    val mosaic: List<CoverArt>,
    val artist: String,
) {
    val songs: List<OfflineSong> = tracks.mapNotNull { it.song }
    val total: Int get() = tracks.size
    val downloaded: Int get() = songs.size
    val isCollection: Boolean get() = kind == LibraryKind.Playlist || kind == LibraryKind.Album || kind == LibraryKind.Liked
}

@Immutable
internal class OfflineLibrary(
    val songs: List<OfflineSong>,
    val items: List<LibraryItem>,
) {
    private val byKey = items.associateBy { it.key }

    fun item(key: String?): LibraryItem? = key?.let { byKey[it] }

    val isEmpty: Boolean get() = items.isEmpty()
}

/**
 * Builds the offline library: the downloaded songs grouped like the downloads manager does
 * (playlist or album they came with, then album, then single songs), with the saved track list
 * of each playlist or album so songs that are not downloaded can still be listed.
 */
internal fun buildOfflineLibrary(
    songs: List<OfflineSong>,
    manifests: List<CollectionManifest>,
    collectionCovers: Map<String, File>,
): OfflineLibrary {
    val byId = LinkedHashMap<String, OfflineSong>(songs.size)
    val rank = HashMap<String, Int>(songs.size)
    val artByAlbum = HashMap<String, CoverArt>()
    songs.forEachIndexed { index, song ->
        byId.putIfAbsent(song.id, song)
        rank.putIfAbsent(song.id, index)
        val cover = song.coverFile
        if (song.album.isNotBlank() && cover != null) {
            artByAlbum.putIfAbsent(song.album.lowercase(), CoverArt(cover))
        }
    }

    val groups = LinkedHashMap<String, MutableList<OfflineSong>>()
    for (song in songs) {
        val key = song.collection.ifBlank { song.album.ifBlank { SINGLES_KEY } }
        groups.getOrPut(key) { mutableListOf() }.add(song)
    }
    val manifestByName = LinkedHashMap<String, CollectionManifest>()
    manifests.forEach { manifestByName.putIfAbsent(it.name, it) }

    val names = LinkedHashSet<String>(groups.keys).apply {
        addAll(manifestByName.keys)
        remove(SINGLES_KEY)
    }

    class Ranked(val item: LibraryItem, val rank: Int, val savedAt: Long)

    val ranked = ArrayList<Ranked>(names.size)
    for (name in names) {
        val owned = groups[name].orEmpty()
        val manifest = manifestByName[name]
        val collectionArt = collectionCovers[name]?.let { CoverArt(it) }
        val kind = when {
            manifest?.kind == CollectionManifest.KIND_LIKED -> LibraryKind.Liked
            manifest?.kind == CollectionManifest.KIND_ALBUM -> LibraryKind.Album
            manifest != null -> LibraryKind.Playlist
            owned.all { it.collection.isBlank() } -> LibraryKind.Album
            owned.all { it.album.equals(name, ignoreCase = true) } -> LibraryKind.Album
            else -> LibraryKind.Playlist
        }

        val seen = HashSet<String>()
        val tracks = ArrayList<LibraryTrack>((manifest?.tracks?.size ?: 0) + owned.size)
        manifest?.tracks?.forEach { t ->
            if (!seen.add(t.trackId)) return@forEach
            val song = byId[t.trackId]
            val album = t.album.ifBlank { song?.album.orEmpty() }
            tracks.add(
                LibraryTrack(
                    key = t.trackId,
                    title = t.title.ifBlank { song?.title.orEmpty() },
                    artist = t.artist.ifBlank { song?.artist.orEmpty() },
                    album = album,
                    song = song,
                    art = song?.coverArt()
                        ?: artByAlbum[album.lowercase()]
                        ?: collectionArt.takeIf { kind == LibraryKind.Album },
                )
            )
        }
        for (song in owned) {
            if (!seen.add(song.id)) continue
            tracks.add(LibraryTrack(song.id, song.title, song.artist, song.album, song, song.coverArt()))
        }
        if (tracks.isEmpty()) continue

        val mosaic = if (collectionArt == null && kind == LibraryKind.Playlist) {
            tracks.asSequence()
                .mapNotNull { t -> t.song?.coverFile?.let { t.album.lowercase() to CoverArt(it) } }
                .distinctBy { it.first }
                .take(4)
                .map { it.second }
                .toList()
                .takeIf { it.size == 4 }
                .orEmpty()
        } else {
            emptyList()
        }
        val firstArt = tracks.firstNotNullOfOrNull { t -> t.song?.coverArt() ?: t.art }
        val artist = tracks.asSequence()
            .map { it.artist }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            .orEmpty()

        ranked.add(
            Ranked(
                item = LibraryItem(
                    key = "c:$name",
                    name = name,
                    kind = kind,
                    tracks = tracks,
                    owned = owned,
                    cover = collectionArt ?: firstArt,
                    mosaic = mosaic,
                    artist = artist,
                ),
                rank = tracks.minOfOrNull { t -> t.song?.let { rank[it.id] } ?: Int.MAX_VALUE } ?: Int.MAX_VALUE,
                savedAt = manifest?.savedAt ?: 0L,
            )
        )
    }

    val items = ArrayList<LibraryItem>(ranked.size + 2)
    if (songs.isNotEmpty()) {
        items.add(
            LibraryItem(
                key = DOWNLOADS_KEY,
                name = "",
                kind = LibraryKind.Downloads,
                tracks = songs.map { LibraryTrack(it.id, it.title, it.artist, it.album, it, it.coverArt()) },
                owned = songs,
                cover = null,
                mosaic = emptyList(),
                artist = "",
            )
        )
    }
    val (liked, rest) = ranked.partition { it.item.kind == LibraryKind.Liked }
    val order = compareBy<Ranked>({ it.rank }, { -it.savedAt })
    liked.sortedWith(order).forEach { items.add(it.item) }
    rest.sortedWith(order).forEach { items.add(it.item) }

    groups[SINGLES_KEY]?.takeIf { it.isNotEmpty() }?.let { singles ->
        items.add(
            LibraryItem(
                key = SINGLES_KEY,
                name = "",
                kind = LibraryKind.Singles,
                tracks = singles.map { LibraryTrack(it.id, it.title, it.artist, it.album, it, it.coverArt()) },
                owned = singles,
                cover = singles.firstOrNull()?.coverArt(),
                mosaic = emptyList(),
                artist = "",
            )
        )
    }
    return OfflineLibrary(songs = songs, items = items)
}

internal fun LibraryItem.matchesFilter(filter: LibraryFilter?): Boolean = when (filter) {
    null -> true
    LibraryFilter.Playlists -> kind == LibraryKind.Downloads || kind == LibraryKind.Liked ||
        kind == LibraryKind.Playlist || kind == LibraryKind.Singles
    LibraryFilter.Albums -> kind == LibraryKind.Album
    LibraryFilter.Songs -> false
}
