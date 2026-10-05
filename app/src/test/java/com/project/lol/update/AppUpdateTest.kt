package com.project.lol.update

import com.project.lol.util.GitHubApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {

    @Test
    fun parsesCiReleaseTags() {
        assertEquals(ReleaseTag(listOf(2, 10, 1), 20), ReleaseTag.parse("v2.10.1-build20"))
        assertEquals(ReleaseTag(listOf(2, 10, 1), 0), ReleaseTag.parse("v2.10.1"))
        assertEquals(ReleaseTag(listOf(2, 10, 1), 0), ReleaseTag.parse("2.10.1"))
        assertEquals(ReleaseTag(listOf(3, 0), 7), ReleaseTag.parse("V3.0-Build7"))
        assertEquals("2.10.1", ReleaseTag.parse("v2.10.1-build20")?.versionName)
        assertNull(ReleaseTag.parse("nightly"))
        assertNull(ReleaseTag.parse(""))
    }

    @Test
    fun patchReleasesAreNewer() {
        // The old checker read "1-build20" as 0, so 2.10.1 never beat 2.10.0.
        assertTrue(ReleaseTag.isNewer(tag("v2.10.1-build20"), "2.10.0", 18))
        assertTrue(ReleaseTag.isNewer(tag("v2.10.2-build21"), "2.10.1", 20))
        assertTrue(ReleaseTag.isNewer(tag("v2.11.0-build5"), "2.10.1", 20))
        assertTrue(ReleaseTag.isNewer(tag("v3.0-build1"), "2.10.1", 0))
    }

    @Test
    fun sameVersionComparesBuilds() {
        assertTrue(ReleaseTag.isNewer(tag("v2.10.1-build21"), "2.10.1", 20))
        assertFalse(ReleaseTag.isNewer(tag("v2.10.1-build20"), "2.10.1", 20))
        assertFalse(ReleaseTag.isNewer(tag("v2.10.1-build19"), "2.10.1", 20))
        assertFalse(ReleaseTag.isNewer(tag("v2.10-build21"), "2.10.0", 21))
    }

    @Test
    fun localBuildsOnlyUpdateToANewerVersion() {
        assertFalse(ReleaseTag.isNewer(tag("v2.10.1-build99"), "2.10.1", 0))
        assertTrue(ReleaseTag.isNewer(tag("v2.10.2-build1"), "2.10.1", 0))
    }

    @Test
    fun olderReleasesAreNotNewer() {
        assertFalse(ReleaseTag.isNewer(tag("v2.9.0-build50"), "2.10.1", 20))
        assertFalse(ReleaseTag.isNewer(tag("v2.10.1-build20"), "not a version", 20))
    }

    @Test
    fun releaseJsonBecomesAnUpdate() {
        val json = JSONObject(
            """
            {
              "tag_name": "v2.10.2-build21",
              "name": "SpotiOS 2.10.2 (build 21)",
              "body": "Faster **updates**\n\n---\nBuilt from `abc`.\nSigning certificate SHA-256: `00ff`",
              "html_url": "https://github.com/hacker123as/SpotiOS/releases/tag/v2.10.2-build21",
              "published_at": "2026-10-05T12:00:00Z",
              "assets": [
                { "name": "notes.txt", "browser_download_url": "https://example.com/notes.txt", "size": 10 },
                { "name": "SpotiOS-v2.10.2-21.apk", "browser_download_url": "https://github.com/hacker123as/SpotiOS/releases/download/v2.10.2-build21/SpotiOS-v2.10.2-21.apk", "size": 14567890 }
              ]
            }
            """.trimIndent()
        )
        val update = AppUpdate.from(GitHubApi.parseRelease(json))
        assertNotNull(update)
        update!!
        assertEquals("2.10.2", update.versionName)
        assertEquals(21, update.build)
        assertEquals("SpotiOS-v2.10.2-21.apk", update.apkName)
        assertEquals(14567890L, update.apkSize)
        assertTrue(update.apkUrl!!.endsWith("/SpotiOS-v2.10.2-21.apk"))
        assertEquals("Faster **updates**", update.notes)
    }

    @Test
    fun releaseWithoutApkOrBody() {
        val json = JSONObject("""{ "tag_name": "v2.10.2-build21", "body": null, "assets": [] }""")
        val release = GitHubApi.parseRelease(json)
        assertEquals("", release.body)
        val update = AppUpdate.from(release)!!
        assertNull(update.apkUrl)
        assertEquals("", update.notes)
    }

    @Test
    fun notesWithoutFooterKeepTheWholeBody() {
        assertEquals("Line one\nLine two", AppUpdate.notesFrom("Line one\r\nLine two\n"))
    }

    private fun tag(text: String) = ReleaseTag.parse(text)!!
}
