package com.project.lol.update

import com.project.lol.util.GitHubRelease

/** A release tag such as `v2.10.1-build20`: the app version and the CI build number (0 if absent). */
data class ReleaseTag(val version: List<Int>, val build: Int) {

    val versionName: String get() = version.joinToString(".")

    companion object {
        private val TAG_PATTERN = Regex("""^v?(\d+(?:\.\d+)*)(?:-build(\d+))?""", RegexOption.IGNORE_CASE)

        fun parse(tag: String): ReleaseTag? {
            val match = TAG_PATTERN.find(tag.trim()) ?: return null
            val version = match.groupValues[1].split('.').map { it.toIntOrNull() ?: return null }
            val build = match.groupValues[2].takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: return null } ?: 0
            return ReleaseTag(version, build)
        }

        fun compareVersions(a: List<Int>, b: List<Int>): Int {
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x.compareTo(y)
            }
            return 0
        }

        /**
         * Whether [latest] is newer than the running app. A local build (build 0) doesn't know
         * which CI build it matches, so the same version counts as up to date there.
         */
        fun isNewer(latest: ReleaseTag, currentVersion: String, currentBuild: Int): Boolean {
            val current = parse(currentVersion) ?: return false
            val byVersion = compareVersions(latest.version, current.version)
            if (byVersion != 0) return byVersion > 0
            return currentBuild > 0 && latest.build > currentBuild
        }
    }
}

/** A newer SpotiOS on GitHub Releases. */
data class AppUpdate(
    val tag: String,
    val versionName: String,
    val build: Int,
    /** Markdown: the part of the release body above the CI's `---` footer. */
    val notes: String,
    val pageUrl: String,
    val apkName: String?,
    val apkUrl: String?,
    val apkSize: Long
) {
    companion object {
        fun from(release: GitHubRelease): AppUpdate? {
            val tag = ReleaseTag.parse(release.tagName) ?: return null
            val apk = release.apk?.takeIf { it.downloadUrl.isNotBlank() }
            return AppUpdate(
                tag = release.tagName,
                versionName = tag.versionName,
                build = tag.build,
                notes = notesFrom(release.body),
                pageUrl = release.htmlUrl,
                apkName = apk?.name,
                apkUrl = apk?.downloadUrl,
                apkSize = apk?.size ?: 0L
            )
        }

        /** The release body up to the first `---` line, where CI puts download and signing details. */
        fun notesFrom(body: String): String {
            val lines = body.replace("\r\n", "\n").lines()
            val footer = lines.indexOfFirst { it.trim() == "---" }
            return (if (footer >= 0) lines.take(footer) else lines).joinToString("\n").trim()
        }
    }
}
