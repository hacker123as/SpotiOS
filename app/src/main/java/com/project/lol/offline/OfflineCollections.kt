package com.project.lol.offline

import android.content.Context
import com.project.lol.util.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** One song of a playlist or album, whether it ended up on the device or not. */
data class CollectionTrack(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSec: Int? = null,
    val cover: String? = null,
)

/**
 * The full track list of a playlist or album, saved when its download starts. Downloads only
 * keep the songs that made it to disk, so without this the offline library could not show the
 * rest of the playlist (grayed out, like Spotify does offline).
 */
data class CollectionManifest(
    val name: String,
    val kind: String = KIND_PLAYLIST,
    val cover: String? = null,
    val tracks: List<CollectionTrack> = emptyList(),
    val savedAt: Long = 0L,
) {
    companion object {
        const val KIND_PLAYLIST = "playlist"
        const val KIND_ALBUM = "album"
        const val KIND_LIKED = "liked"

        fun kindFromType(type: String?): String = when (type?.trim()?.lowercase()) {
            KIND_ALBUM -> KIND_ALBUM
            KIND_LIKED -> KIND_LIKED
            else -> KIND_PLAYLIST
        }
    }
}

/**
 * Small JSON manifests in app storage, one per playlist or album:
 * filesDir/offline_collections/<safe-name>.json, plus <safe-name>.jpg for the cover.
 * Collections downloaded by older versions have no manifest; the library falls back to
 * their downloaded songs.
 */
object OfflineCollections {
    private const val TAG = "Spl-DL"
    private const val DIR = "offline_collections"
    private const val FORMAT_VERSION = 1

    fun dir(context: Context): File = File(context.filesDir, DIR)

    /** Stable, file-system safe base name. The hash keeps names that slug the same apart. */
    fun safeName(name: String): String {
        val slug = name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(40)
            .trimEnd('-')
            .ifBlank { "collection" }
        val hash = MessageDigest.getInstance("SHA-1")
            .digest(name.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(10)
        return "$slug-$hash"
    }

    private fun manifestFile(context: Context, name: String) = File(dir(context), "${safeName(name)}.json")

    private fun coverTarget(context: Context, name: String) = File(dir(context), "${safeName(name)}.jpg")

    /** The saved cover image of a collection, or null if there is none. */
    fun coverFile(context: Context, name: String): File? =
        coverTarget(context, name).takeIf { it.exists() && it.length() > 0 }

    fun toJson(manifest: CollectionManifest): String {
        val tracks = JSONArray()
        for (t in manifest.tracks) {
            tracks.put(
                JSONObject().apply {
                    put("trackId", t.trackId)
                    put("title", t.title)
                    put("artist", t.artist)
                    if (t.album.isNotBlank()) put("album", t.album)
                    if (t.durationSec != null && t.durationSec > 0) put("durationSec", t.durationSec)
                    if (!t.cover.isNullOrBlank()) put("cover", t.cover)
                }
            )
        }
        return JSONObject().apply {
            put("version", FORMAT_VERSION)
            put("name", manifest.name)
            put("kind", manifest.kind)
            if (!manifest.cover.isNullOrBlank()) put("cover", manifest.cover)
            put("savedAt", manifest.savedAt)
            put("tracks", tracks)
        }.toString()
    }

    /** Parses a manifest; null when the text is not one. Unknown fields are ignored. */
    fun fromJson(text: String): CollectionManifest? = runCatching {
        val root = JSONObject(text)
        val name = root.optString("name").trim()
        if (name.isEmpty()) return@runCatching null
        val arr = root.optJSONArray("tracks") ?: JSONArray()
        val seen = HashSet<String>()
        val tracks = ArrayList<CollectionTrack>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("trackId").trim()
            if (id.isEmpty() || !seen.add(id)) continue
            tracks.add(
                CollectionTrack(
                    trackId = id,
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    album = o.optString("album"),
                    durationSec = o.optInt("durationSec", 0).takeIf { it > 0 },
                    cover = o.optString("cover").ifBlank { null },
                )
            )
        }
        CollectionManifest(
            name = name,
            kind = CollectionManifest.kindFromType(root.optString("kind")),
            cover = root.optString("cover").ifBlank { null },
            tracks = tracks,
            savedAt = root.optLong("savedAt", 0L),
        )
    }.getOrNull()

    /** Writes the manifest (replacing an older one with the same name). Safe to call off the main thread only. */
    fun save(context: Context, manifest: CollectionManifest): Boolean = runCatching {
        val folder = dir(context).apply { mkdirs() }
        val target = manifestFile(context, manifest.name)
        val tmp = File(folder, "${target.name}.tmp")
        tmp.writeText(toJson(manifest))
        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText())
            tmp.delete()
        }
        true
    }.getOrElse {
        Logger.w(TAG, "OfflineCollections.save: failed for '${manifest.name}': ${it.message}")
        false
    }

    fun load(context: Context, name: String): CollectionManifest? {
        val file = manifestFile(context, name)
        if (!file.exists()) return null
        return runCatching { fromJson(file.readText()) }.getOrNull()
    }

    /** Every saved manifest, newest first. One per name. */
    fun loadAll(context: Context): List<CollectionManifest> {
        val files = dir(context).listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyList()
        return files.mapNotNull { f -> runCatching { fromJson(f.readText()) }.getOrNull() }
            .sortedByDescending { it.savedAt }
            .distinctBy { it.name }
    }

    /** Removes the manifest and cover of a collection. TRUE if anything was removed. */
    fun delete(context: Context, name: String): Boolean {
        if (name.isBlank()) return false
        var removed = false
        runCatching { if (manifestFile(context, name).delete()) removed = true }
        runCatching { if (coverTarget(context, name).delete()) removed = true }
        return removed
    }

    fun deleteAll(context: Context) {
        runCatching { dir(context).listFiles()?.forEach { it.delete() } }
    }

    /** Best effort: keeps the collection cover so the library has artwork offline. Network, call off the main thread. */
    fun fetchCover(context: Context, name: String, url: String?) {
        if (url.isNullOrBlank() || coverFile(context, name) != null) return
        val target = coverTarget(context, name)
        runCatching {
            target.parentFile?.mkdirs()
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true
            try {
                conn.inputStream.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            } finally {
                conn.disconnect()
            }
            if (target.length() == 0L) target.delete()
        }.onFailure {
            Logger.w(TAG, "OfflineCollections.fetchCover: failed for '$name': ${it.message}")
            runCatching { target.delete() }
        }
    }
}
