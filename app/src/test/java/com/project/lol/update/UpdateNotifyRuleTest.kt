package com.project.lol.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNotifyRuleTest {

    @Test
    fun firstBuildIsNotified() {
        assertTrue(UpdateNotifyRule.shouldNotify(update("v3.0.1-build40"), lastBuild = 0, lastTag = null))
    }

    @Test
    fun sameBuildIsNotNotifiedTwice() {
        assertFalse(UpdateNotifyRule.shouldNotify(update("v3.0.1-build40"), lastBuild = 40, lastTag = "v3.0.1-build40"))
    }

    @Test
    fun newerBuildIsNotified() {
        assertTrue(UpdateNotifyRule.shouldNotify(update("v3.0.1-build41"), lastBuild = 40, lastTag = "v3.0.1-build40"))
        assertTrue(UpdateNotifyRule.shouldNotify(update("v3.1.0-build45"), lastBuild = 40, lastTag = "v3.0.1-build40"))
    }

    @Test
    fun olderBuildIsNotNotified() {
        // A release pulled back to an older build after a newer one was announced.
        assertFalse(UpdateNotifyRule.shouldNotify(update("v3.0.1-build39"), lastBuild = 40, lastTag = "v3.0.1-build40"))
    }

    @Test
    fun tagsWithoutBuildCompareByTag() {
        assertTrue(UpdateNotifyRule.shouldNotify(update("v3.0.1"), lastBuild = 0, lastTag = null))
        assertFalse(UpdateNotifyRule.shouldNotify(update("v3.0.1"), lastBuild = 0, lastTag = "v3.0.1"))
        assertTrue(UpdateNotifyRule.shouldNotify(update("v3.0.2"), lastBuild = 0, lastTag = "v3.0.1"))
    }

    private fun update(tag: String): AppUpdate {
        val parsed = ReleaseTag.parse(tag)!!
        return AppUpdate(
            tag = tag,
            versionName = parsed.versionName,
            build = parsed.build,
            notes = "",
            pageUrl = "",
            apkName = null,
            apkUrl = null,
            apkSize = 0L
        )
    }
}
