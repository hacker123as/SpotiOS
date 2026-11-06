package com.project.lol.security

import com.project.lol.util.Logger
import com.project.lol.webview.LogoutGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardeningTest {

    @Test fun nativeFetchOnlyReachesSpotify() {
        assertTrue(com.project.lol.bridge.NativeNet.allowed("https://gew4-spclient.spotify.com/connect-state/v1/cluster"))
        assertTrue(com.project.lol.bridge.NativeNet.allowed("https://api-partner.spotify.com/pathfinder/v2/query"))
        assertTrue(com.project.lol.bridge.NativeNet.allowed("https://i.scdn.co/image/ab67"))
        assertTrue(com.project.lol.bridge.NativeNet.allowed("https://open.spotifycdn.com/cdn/x.js"))
        assertTrue(com.project.lol.bridge.NativeNet.allowed("https://spotify.com/"))
        assertFalse(com.project.lol.bridge.NativeNet.allowed("http://api.spotify.com/v1/me"))
        assertFalse(com.project.lol.bridge.NativeNet.allowed("https://evil.com/?spotify.com"))
        assertFalse(com.project.lol.bridge.NativeNet.allowed("https://spotify.com.evil.com/"))
        assertFalse(com.project.lol.bridge.NativeNet.allowed("https://evilspotify.com/"))
        assertFalse(com.project.lol.bridge.NativeNet.allowed("https://api.spotify.com@evil.com/"))
        assertFalse(com.project.lol.bridge.NativeNet.allowed("https://user@api.spotify.com/"))
        assertFalse(com.project.lol.bridge.NativeNet.allowed("file:///data/data/com.spotios.app/shared_prefs/x.xml"))
        assertFalse(com.project.lol.bridge.NativeNet.allowed("https://192.168.1.1/"))
        assertFalse(com.project.lol.bridge.NativeNet.allowed(""))
        assertFalse(com.project.lol.bridge.NativeNet.allowed(null))
        assertFalse(com.project.lol.bridge.NativeNet.allowed("not a url"))
    }

    @Test fun oneLoggedOutPageOnlyReloads() {
        LogoutGuard.clear()
        assertFalse(LogoutGuard.confirm(1_000L))
        assertTrue(LogoutGuard.confirm(60_000L))
        // after a confirmed logout it starts over
        assertFalse(LogoutGuard.confirm(70_000L))
        // a blip long ago doesn't count
        assertFalse(LogoutGuard.confirm(70_000L + 11 * 60_000L))
        LogoutGuard.clear()
        assertFalse(LogoutGuard.confirm(2_000_000L))
    }

    @Test fun logsHideTokens() {
        val tok = "BQDx9_abcDEF-123456789xyz"
        assertEquals("wss://dealer.spotify.com/?access_token=<hidden>", Logger.redact("wss://dealer.spotify.com/?access_token=$tok"))
        assertEquals("Authorization: Bearer <hidden>", Logger.redact("Authorization: Bearer $tok"))
        assertEquals("cookie sp_dc=<hidden>; x=1", Logger.redact("cookie sp_dc=$tok; x=1"))
        assertEquals("{\"client-token\":\"<hidden>\"}", Logger.redact("{\"client-token\":\"$tok\"}"))
        assertEquals("nothing secret here", Logger.redact("nothing secret here"))
    }
}

class SafeNavTest {
    @Test fun playerOnlyShowsSpotifyAndSignIn() {
        val ok = listOf("open.spotify.com", "accounts.spotify.com", "www.spotify.com", "spotify.link", "challenge.spotify.com",
            "accounts.google.com", "accounts.google.de", "accounts.google.co.uk", "accounts.youtube.com",
            "www.facebook.com", "m.facebook.com", "appleid.apple.com", "www.recaptcha.net", "OPEN.SPOTIFY.COM", "open.spotify.com.")
        ok.forEach { assertTrue(it, com.project.lol.webview.SafeNav.allowedHost(it)) }
        val bad = listOf("evil.com", "spotify.com.evil.com", "notspotify.com", "accounts.google.evil.com", "www.youtube.com",
            "evil.google.attacker.com", "apple.com.evil.net", "", null)
        bad.forEach { assertFalse(it.toString(), com.project.lol.webview.SafeNav.allowedHost(it)) }
    }
}
