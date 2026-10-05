package com.project.lol.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCollectionsTest {

    private val sample = CollectionManifest(
        name = "Late Night \"Drive\" / Mix",
        kind = CollectionManifest.KIND_ALBUM,
        cover = "https://i.scdn.co/image/ab67616d0000b273aaaa",
        tracks = listOf(
            CollectionTrack(
                trackId = "4uLU6hMCjMI75M1A2tKUQC",
                title = "Song \"One\"",
                artist = "Artist A, Artist B",
                album = "Album",
                durationSec = 215,
                cover = "https://i.scdn.co/image/ab67616d0000b273bbbb",
            ),
            CollectionTrack(trackId = "7ouMYWpwJ422jRcDASZB7P", title = "Två", artist = "Ö"),
        ),
        savedAt = 1_700_000_000_000L,
    )

    @Test
    fun roundTripKeepsEveryField() {
        val back = OfflineCollections.fromJson(OfflineCollections.toJson(sample))
        assertEquals(sample, back)
    }

    @Test
    fun emptyTrackListRoundTrips() {
        val empty = CollectionManifest(name = "Empty", savedAt = 5L)
        assertEquals(empty, OfflineCollections.fromJson(OfflineCollections.toJson(empty)))
    }

    @Test
    fun duplicateAndBlankTrackIdsAreDropped() {
        val json = """
            {"name":"P","kind":"playlist","tracks":[
              {"trackId":"a","title":"A","artist":"x"},
              {"trackId":"","title":"no id"},
              {"title":"missing id"},
              {"trackId":"a","title":"A again"},
              {"trackId":"b","title":"B","artist":"y","durationSec":0}
            ]}
        """.trimIndent()
        val parsed = OfflineCollections.fromJson(json)
        assertNotNull(parsed)
        assertEquals(listOf("a", "b"), parsed!!.tracks.map { it.trackId })
        assertEquals("A", parsed.tracks[0].title)
        assertNull(parsed.tracks[1].durationSec)
    }

    @Test
    fun invalidInputIsRejected() {
        assertNull(OfflineCollections.fromJson("not json"))
        assertNull(OfflineCollections.fromJson("{}"))
        assertNull(OfflineCollections.fromJson("""{"name":"   ","tracks":[]}"""))
    }

    @Test
    fun unknownKindFallsBackToPlaylist() {
        val parsed = OfflineCollections.fromJson("""{"name":"X","kind":"podcast"}""")
        assertEquals(CollectionManifest.KIND_PLAYLIST, parsed?.kind)
        assertEquals(CollectionManifest.KIND_LIKED, CollectionManifest.kindFromType("Liked"))
        assertEquals(CollectionManifest.KIND_ALBUM, CollectionManifest.kindFromType("album"))
    }

    @Test
    fun safeNameIsStableAndFileSafe() {
        val names = listOf("My Playlist #1", "my playlist 1", "AC/DC: Back in Black", "日本の歌", "", "..")
        for (n in names) {
            val safe = OfflineCollections.safeName(n)
            assertTrue("'$safe' is not file safe", safe.matches(Regex("[a-z0-9-]+")))
            assertEquals(safe, OfflineCollections.safeName(n))
        }
        // Names that slug the same still get different files.
        assertNotEquals(
            OfflineCollections.safeName("My Playlist #1"),
            OfflineCollections.safeName("my playlist 1"),
        )
        assertTrue(OfflineCollections.safeName("日本の歌").startsWith("collection-"))
    }
}
