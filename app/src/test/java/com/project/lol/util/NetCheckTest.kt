package com.project.lol.util

import com.project.lol.util.NetCheckRules.Problem
import com.project.lol.util.NetCheckRules.TlsOutcome
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

class NetCheckTest {

    /** Literal addresses only: InetAddress parses these without DNS. */
    private fun ip(literal: String): ByteArray = InetAddress.getByName(literal).address

    private fun ipv6(vararg bytes: Int): ByteArray = ByteArray(16).also { b -> bytes.forEachIndexed { i, v -> b[i] = v.toByte() } }

    private val google = listOf(ip("142.251.155.119"))
    private val spotify = listOf(ip("35.186.224.24"), ip("2600:1901:1:c36::"))

    private fun status(
        vpn: Boolean = false,
        privateDns: String = "",
        blockers: List<String> = emptyList(),
        blocked: List<String> = emptyList(),
        tls: Boolean = false,
        intercepted: Boolean = false
    ) = NetStatus(vpn, privateDns, blockers, blocked, tls, intercepted, checkedAt = 1_700_000_000_000L)

    @Test
    fun sinkholeAddresses() {
        assertTrue(NetCheckRules.isSinkhole(ip("0.0.0.0")))
        assertTrue(NetCheckRules.isSinkhole(ip("0.1.2.3")))
        assertTrue(NetCheckRules.isSinkhole(ip("127.0.0.1")))
        assertTrue(NetCheckRules.isSinkhole(ip("127.255.255.254")))
        assertTrue(NetCheckRules.isSinkhole(ip("::")))
        assertTrue(NetCheckRules.isSinkhole(ip("::1")))
        // ::ffff:0.0.0.0 and ::ffff:127.0.0.1 (InetAddress would fold these to IPv4, so build the bytes).
        assertTrue(NetCheckRules.isSinkhole(ipv6(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0xff, 0xff, 0, 0, 0, 0)))
        assertTrue(NetCheckRules.isSinkhole(ipv6(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0xff, 0xff, 127, 0, 0, 1)))
    }

    @Test
    fun realAddressesAreNotSinkholes() {
        assertFalse(NetCheckRules.isSinkhole(ip("35.186.224.24")))
        assertFalse(NetCheckRules.isSinkhole(ip("151.101.131.42")))
        assertFalse(NetCheckRules.isSinkhole(ip("128.0.0.1")))
        assertFalse(NetCheckRules.isSinkhole(ip("10.0.0.1")))
        assertFalse(NetCheckRules.isSinkhole(ip("2600:1901:1:c36::")))
        assertFalse(NetCheckRules.isSinkhole(ip("::2")))
        assertFalse(NetCheckRules.isSinkhole(ipv6(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0xff, 0xff, 35, 186, 224, 24)))
        assertFalse(NetCheckRules.isSinkhole(ByteArray(0)))
    }

    @Test
    fun blockedAnswers() {
        assertTrue("no answer", NetCheckRules.isBlockedAnswer(null, null))
        assertTrue("empty answer", NetCheckRules.isBlockedAnswer(emptyList(), null))
        assertTrue("0.0.0.0", NetCheckRules.isBlockedAnswer(listOf(ip("0.0.0.0")), null))
        assertTrue("0.0.0.0 and ::", NetCheckRules.isBlockedAnswer(listOf(ip("0.0.0.0"), ip("::")), null))
        assertTrue("sinkhole mixed in", NetCheckRules.isBlockedAnswer(listOf(ip("35.186.224.24"), ip("::")), null))
        assertFalse("real", NetCheckRules.isBlockedAnswer(spotify, null))

        // A blocker that answers blocked names with its own address: the ad canary gets the same one.
        val blockPage = listOf(ip("146.112.61.106"))
        assertTrue(NetCheckRules.isBlockedAnswer(listOf(ip("146.112.61.106")), blockPage))
        assertTrue(NetCheckRules.isBlockedAnswer(listOf(ip("192.168.1.2")), listOf(ip("192.168.1.2"), ip("fd00::2"))))
        assertFalse(NetCheckRules.isBlockedAnswer(spotify, blockPage))
        // An unblocked canary resolves to Google's ad servers, nothing like Spotify's.
        assertFalse(NetCheckRules.isBlockedAnswer(spotify, listOf(ip("172.217.214.156"))))
        // Only part of the answer matches the canary: not counted.
        assertFalse(NetCheckRules.isBlockedAnswer(listOf(ip("146.112.61.106"), ip("35.186.224.24")), blockPage))
    }

    @Test
    fun blockedHostsNeedWorkingDns() {
        val answers = linkedMapOf<String, List<ByteArray>?>(
            "apresolve.spotify.com" to spotify,
            "dealer.spotify.com" to listOf(ip("0.0.0.0")),
            "spclient.wg.spotify.com" to null,
            "open.spotify.com" to listOf(ip("151.101.131.42"))
        )
        assertEquals(listOf("dealer.spotify.com", "spclient.wg.spotify.com"), NetCheckRules.blockedHosts(answers, google, null))
        // No internet: the control name fails too, so nothing is called blocked.
        assertEquals(emptyList<String>(), NetCheckRules.blockedHosts(answers, null, null))
        assertEquals(emptyList<String>(), NetCheckRules.blockedHosts(answers, emptyList(), null))
        assertEquals(emptyList<String>(), NetCheckRules.blockedHosts(answers, listOf(ip("0.0.0.0")), null))
        // Lookups that timed out are left out of answers and aren't counted.
        assertEquals(emptyList<String>(), NetCheckRules.blockedHosts(mapOf("apresolve.spotify.com" to spotify), google, null))
    }

    @Test
    fun dnsWorksOnlyWithRealControlAnswer() {
        assertTrue(NetCheckRules.dnsWorks(google))
        assertFalse(NetCheckRules.dnsWorks(null))
        assertFalse(NetCheckRules.dnsWorks(emptyList()))
        assertFalse(NetCheckRules.dnsWorks(listOf(ip("127.0.0.1"))))
    }

    @Test
    fun okLogic() {
        assertTrue(NetCheckRules.ok(emptyList(), tls = false))
        assertFalse(NetCheckRules.ok(listOf("dealer.spotify.com"), tls = false))
        assertFalse(NetCheckRules.ok(emptyList(), tls = true))
        // A VPN, Private DNS, an installed blocker or a decrypting CA alone isn't a failure.
        assertTrue(status(vpn = true, privateDns = "dns.adguard-dns.com", blockers = listOf("AdGuard"), intercepted = true).ok)
        assertFalse(status(blocked = listOf("apresolve.spotify.com")).ok)
        assertFalse(status(tls = true).ok)
        assertTrue(NetStatus.NONE.ok)
    }

    @Test
    fun tlsOutcomes() {
        val untrusted = SSLHandshakeException("java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.")
            .apply { initCause(CertificateException(CertPathValidatorException("Trust anchor for certification path not found."))) }
        assertEquals(TlsOutcome.CERT, NetCheckRules.tlsOutcome(untrusted))
        assertEquals(TlsOutcome.CERT, NetCheckRules.tlsOutcome(SSLPeerUnverifiedException("Hostname apresolve.spotify.com not verified")))
        assertEquals(TlsOutcome.CERT, NetCheckRules.tlsOutcome(IOException(CertPathValidatorException("expired"))))
        assertEquals(TlsOutcome.HANDSHAKE, NetCheckRules.tlsOutcome(SSLHandshakeException("Connection closed by peer")))
        assertEquals(TlsOutcome.OTHER, NetCheckRules.tlsOutcome(SocketTimeoutException("timeout")))
        assertEquals(TlsOutcome.OTHER, NetCheckRules.tlsOutcome(IOException("Connection refused")))
    }

    @Test
    fun tlsProblemNeedsCertErrorOrOnlyBrokenHandshakes() {
        assertFalse(NetCheckRules.tlsProblem(emptyList()))
        assertFalse(NetCheckRules.tlsProblem(listOf(TlsOutcome.OK)))
        assertTrue(NetCheckRules.tlsProblem(listOf(TlsOutcome.CERT)))
        assertTrue(NetCheckRules.tlsProblem(listOf(TlsOutcome.HANDSHAKE, TlsOutcome.CERT)))
        assertTrue(NetCheckRules.tlsProblem(listOf(TlsOutcome.HANDSHAKE, TlsOutcome.HANDSHAKE)))
        assertTrue(NetCheckRules.tlsProblem(listOf(TlsOutcome.HANDSHAKE)))
        // One flaky handshake followed by a good answer, or a plain timeout: fine.
        assertFalse(NetCheckRules.tlsProblem(listOf(TlsOutcome.HANDSHAKE, TlsOutcome.OK)))
        assertFalse(NetCheckRules.tlsProblem(listOf(TlsOutcome.OTHER, TlsOutcome.HANDSHAKE)))
        assertFalse(NetCheckRules.tlsProblem(listOf(TlsOutcome.OTHER)))
    }

    @Test
    fun interceptionByAdGuardOrUserCa() {
        val leaf = "CN=apresolve.spotify.com"
        val realIssuer = "cn=gts ca 1c3,o=google trust services llc,c=us"
        assertFalse(NetCheckRules.looksIntercepted(listOf(leaf, realIssuer), realIssuer, "cn=apresolve.spotify.com", emptySet()))
        assertTrue(NetCheckRules.looksIntercepted(listOf(leaf, "CN=AdGuard Personal CA 1A2B3C"), "cn=adguard personal ca 1a2b3c", "cn=apresolve.spotify.com", emptySet()))
        val userCa = "cn=corp proxy root,o=example"
        assertTrue(NetCheckRules.looksIntercepted(listOf(leaf, "CN=Corp Proxy Root"), userCa, "cn=apresolve.spotify.com", setOf(userCa)))
    }

    @Test
    fun problemLineNamesTheOneBlocker() {
        assertNull(NetCheckRules.problem(status(blockers = listOf("AdGuard"))))
        assertEquals(Problem.APP, NetCheckRules.problem(status(vpn = true, blockers = listOf("AdGuard"), tls = true)))
        assertEquals(Problem.APP, NetCheckRules.problem(status(vpn = true, blockers = listOf("AdGuard"), blocked = listOf("dealer.spotify.com"))))
        assertEquals(Problem.GENERIC, NetCheckRules.problem(status(vpn = true, blockers = listOf("AdGuard", "NetGuard"), tls = true)))
        assertEquals(Problem.GENERIC, NetCheckRules.problem(status(vpn = true, blocked = listOf("dealer.spotify.com"))))
        // Private DNS doing the blocking, no VPN in the way.
        assertEquals(
            Problem.PRIVATE_DNS,
            NetCheckRules.problem(status(privateDns = "dns.adguard-dns.com", blockers = listOf("NetGuard"), blocked = listOf("dealer.spotify.com")))
        )
        assertEquals(
            Problem.APP,
            NetCheckRules.problem(status(vpn = true, privateDns = "dns.adguard-dns.com", blockers = listOf("AdGuard"), blocked = listOf("dealer.spotify.com")))
        )
    }

    @Test
    fun blockerNamesAreKnownAndUnique() {
        val packages = NetCheckRules.BLOCKER_APPS.map { it.first }
        assertEquals(packages.size, packages.toSet().size)
        assertTrue("com.adguard.android" in packages)
        assertEquals("AdGuard", NetCheckRules.BLOCKER_APPS.first { it.first == "com.adguard.android.contentblocker" }.second)
        assertTrue(NetCheckRules.SPOTIFY_HOSTS.containsAll(NetCheckRules.TLS_PROBES.map { it.first }))
    }

    @Test
    fun jsonShape() {
        val json = JSONObject(
            NetCheckRules.toJson(
                status(vpn = true, privateDns = "dns.example", blockers = listOf("AdGuard"), blocked = listOf("dealer.spotify.com"), tls = false),
                checking = true
            )
        )
        assertEquals(
            setOf("vpn", "privateDns", "blockers", "blocked", "tls", "intercepted", "ok", "checkedAt", "checking"),
            json.keys().asSequence().toSet()
        )
        assertTrue(json.getBoolean("vpn"))
        assertEquals("dns.example", json.getString("privateDns"))
        assertEquals("AdGuard", json.getJSONArray("blockers").getString(0))
        assertEquals(1, json.getJSONArray("blockers").length())
        assertEquals("dealer.spotify.com", json.getJSONArray("blocked").getString(0))
        assertFalse(json.getBoolean("tls"))
        assertFalse(json.getBoolean("intercepted"))
        assertFalse(json.getBoolean("ok"))
        assertEquals(1_700_000_000_000L, json.getLong("checkedAt"))
        assertTrue(json.getBoolean("checking"))

        val first = JSONObject(NetCheckRules.toJson(NetStatus.NONE, checking = true))
        assertTrue(first.getBoolean("ok"))
        assertEquals(0L, first.getLong("checkedAt"))
        assertEquals("", first.getString("privateDns"))
        assertEquals(0, first.getJSONArray("blocked").length())
    }
}
