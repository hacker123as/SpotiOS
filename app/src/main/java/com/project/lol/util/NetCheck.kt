package com.project.lol.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Settings
import com.project.lol.R
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.security.KeyStore
import java.security.cert.CertPathValidatorException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLProtocolException
import javax.security.auth.x500.X500Principal

/**
 * Finds out whether a VPN, Private DNS or an ad blocker (AdGuard and friends) is keeping
 * SpotiOS Server from reaching Spotify, so the Server screen and the notification can tell
 * the user what to turn off.
 *
 * [snapshot] answers at once from a cache and refreshes it in the background: one refresh at a
 * time, on its own thread, never on the caller's. DNS and the TLS probe never run on the main
 * thread.
 */
object NetCheck {

    private const val TAG = "netcheck"

    /** A result older than this is checked again on the next [snapshot]. */
    private const val MAX_AGE_MS = 60_000L
    /**
     * [problemText] runs with every Server notification update, all day: while all is well it
     * checks only this often. A VPN or Private DNS change still triggers a check right away.
     */
    private const val QUIET_MAX_AGE_MS = 5 * 60_000L
    /** force=true right after a check gives the same answer instead of checking again. */
    private const val FORCE_GAP_MS = 2_000L
    /** After the network changes (VPN on or off, Private DNS changed), wait this long before checking. */
    private const val SETTLE_MS = 2_000L
    /** All DNS lookups share this budget. A lookup still running after it counts as unknown, not blocked. */
    private const val DNS_BUDGET_MS = 5_000L
    private const val TLS_TIMEOUT_MS = 4_000

    /** One refresh at a time. The thread goes away when idle. */
    private val REFRESH = daemonPool("spo-netcheck", 1)
    /** DNS lookups of one refresh run side by side, so one slow name doesn't hold up the rest. */
    private val DNS = daemonPool("spo-netcheck-dns", 5)

    private val running = AtomicBoolean(false)
    private val watching = AtomicBoolean(false)
    @Volatile private var cached = NetStatus.NONE
    @Volatile private var dirty = false
    @Volatile private var changedAt = 0L

    /**
     * What stands between SpotiOS and Spotify, as JSON:
     * `{"vpn":bool,"privateDns":"<host or empty>","blockers":["AdGuard",...],"blocked":["<spotify host>",...],
     *   "tls":bool,"intercepted":bool,"ok":bool,"checkedAt":<ms>,"checking":bool}`
     *
     * Returns the last result right away (checkedAt 0 before the first check finishes) and starts
     * a check in the background when that result is over 60 s old, the network changed, or
     * [force] is set. `checking` is true while one runs; poll again to get its answer.
     */
    fun snapshot(context: Context, force: Boolean = false): String {
        val app = context.applicationContext ?: context
        watchNetwork(app)
        maybeRefresh(app, force)
        return NetCheckRules.toJson(cached, running.get())
    }

    /**
     * A short line for the Server notification when something is blocking Spotify, else null.
     * Never waits for a check: it reads the last result (and starts a check when that is stale).
     */
    fun problemText(context: Context): String? {
        val app = context.applicationContext ?: context
        watchNetwork(app)
        maybeRefresh(app, false, if (cached.ok) QUIET_MAX_AGE_MS else MAX_AGE_MS)
        val status = cached
        return when (NetCheckRules.problem(status)) {
            null -> null
            NetCheckRules.Problem.APP -> app.getString(R.string.netcheck_problem_app, status.blockers.first())
            NetCheckRules.Problem.PRIVATE_DNS -> app.getString(R.string.netcheck_problem_private_dns)
            NetCheckRules.Problem.GENERIC -> app.getString(R.string.netcheck_problem_generic)
        }
    }

    /**
     * Opens the installed blocker app with this display name (as listed in `blockers`), or its
     * App info page when it has no launcher screen. Null when none with that name is installed.
     */
    fun blockerIntent(context: Context, name: String): Intent? {
        val pm = context.packageManager
        val packages = NetCheckRules.BLOCKER_APPS
            .filter { it.second.equals(name.trim(), ignoreCase = true) }
            .map { it.first }
            .filter { isInstalled(pm, it) }
        packages.firstNotNullOfOrNull { runCatching { pm.getLaunchIntentForPackage(it) }.getOrNull() }
            ?.let { return it }
        return packages.firstOrNull()?.let {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", it, null))
        }
    }

    private fun maybeRefresh(app: Context, force: Boolean, maxAge: Long = MAX_AGE_MS) {
        val last = cached
        val age = System.currentTimeMillis() - last.checkedAt
        val due = last.checkedAt == 0L || dirty || age > maxAge || (force && age >= FORCE_GAP_MS)
        if (!due || !running.compareAndSet(false, true)) return
        try {
            REFRESH.execute {
                try {
                    refresh(app)
                } catch (t: Throwable) {
                    Logger.w(TAG, "check failed: ${t.javaClass.simpleName}: ${t.message}")
                    // Keep the old answer, but don't ask again on every poll.
                    cached = cached.copy(checkedAt = System.currentTimeMillis())
                } finally {
                    running.set(false)
                }
            }
        } catch (e: RejectedExecutionException) {
            running.set(false)
        }
    }

    private fun refresh(app: Context) {
        val sinceChange = System.currentTimeMillis() - changedAt
        if (dirty && sinceChange in 0 until SETTLE_MS) Thread.sleep(SETTLE_MS - sinceChange)
        dirty = false
        val started = System.nanoTime()

        val cm = app.getSystemService(ConnectivityManager::class.java)
        val network = cm?.activeNetwork
        val caps = network?.let { runCatching { cm.getNetworkCapabilities(it) }.getOrNull() }
        val link = network?.let { runCatching { cm.getLinkProperties(it) }.getOrNull() }
        val vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val privateDns = if (link?.isPrivateDnsActive == true) link.privateDnsServerName.orEmpty() else ""
        val blockers = installedBlockers(app.packageManager)

        var blocked = emptyList<String>()
        var tls = false
        var intercepted = false
        if (network != null) {
            val answers = resolveAll(network)
            val control = answers[NetCheckRules.CONTROL_HOST]
            blocked = NetCheckRules.blockedHosts(
                answers.filterKeys { it in NetCheckRules.SPOTIFY_HOSTS },
                control,
                answers[NetCheckRules.AD_CANARY_HOST]
            )
            if (NetCheckRules.dnsWorks(control)) {
                // Probe hosts that resolved fine; a blocked one is already reported above.
                val candidates = NetCheckRules.TLS_PROBES.filter { (host, _) ->
                    answers.containsKey(host) && host !in blocked
                }
                val outcomes = ArrayList<NetCheckRules.TlsOutcome>()
                for ((_, url) in candidates.take(2)) {
                    val (outcome, mitm) = probe(url)
                    outcomes.add(outcome)
                    if (mitm) intercepted = true
                    if (outcome == NetCheckRules.TlsOutcome.OK || outcome == NetCheckRules.TlsOutcome.CERT) break
                }
                tls = NetCheckRules.tlsProblem(outcomes)
            }
        }

        val next = NetStatus(vpn, privateDns, blockers, blocked, tls, intercepted, System.currentTimeMillis())
        val ms = (System.nanoTime() - started) / 1_000_000
        val summary = "vpn=$vpn privateDns=${privateDns.ifEmpty { "-" }} blockers=$blockers " +
            "blocked=$blocked tls=$tls intercepted=$intercepted ok=${next.ok} (${ms}ms)"
        if (next.copy(checkedAt = 0) != cached.copy(checkedAt = 0)) Logger.i(TAG, summary) else Logger.d(TAG, summary)
        cached = next
    }

    /**
     * Looks up the Spotify hosts, the control name and the ad canary on [network], side by side.
     * A name maps to its addresses, or to null when it didn't resolve; a name whose lookup is
     * still running when the budget runs out is left out.
     */
    private fun resolveAll(network: Network): Map<String, List<ByteArray>?> {
        val names = NetCheckRules.SPOTIFY_HOSTS + NetCheckRules.CONTROL_HOST + NetCheckRules.AD_CANARY_HOST
        val pending = LinkedHashMap<String, Future<List<ByteArray>?>>()
        for (name in names) {
            pending[name] = try {
                DNS.submit(Callable {
                    // Looked up on this network (its own cache), as the WebView would.
                    runCatching { network.getAllByName(name).map { it.address } }.getOrNull()
                })
            } catch (e: RejectedExecutionException) {
                continue
            }
        }
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DNS_BUDGET_MS)
        val answers = LinkedHashMap<String, List<ByteArray>?>()
        for ((name, future) in pending) {
            val left = deadline - System.nanoTime()
            val answer = runCatching { future.get(left.coerceAtLeast(0), TimeUnit.NANOSECONDS) }
            if (answer.isSuccess) answers[name] = answer.getOrNull() else future.cancel(true)
        }
        return answers
    }

    /**
     * One HTTPS request with normal certificate checks. Any HTTP status means TLS works; a failed
     * handshake while DNS works is what HTTPS filtering (AdGuard and the like) looks like. Also says
     * whether the chain came from a user-installed CA, i.e. something is decrypting the traffic.
     */
    private fun probe(url: String): Pair<NetCheckRules.TlsOutcome, Boolean> {
        var conn: HttpsURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpsURLConnection).apply {
                connectTimeout = TLS_TIMEOUT_MS
                readTimeout = TLS_TIMEOUT_MS
                requestMethod = "GET"
                useCaches = false
                instanceFollowRedirects = false
            }
            conn.responseCode
            val chain = runCatching { conn.serverCertificates }.getOrNull()
            NetCheckRules.TlsOutcome.OK to (chain != null && isIntercepted(chain))
        } catch (e: Exception) {
            val outcome = NetCheckRules.tlsOutcome(e)
            if (outcome != NetCheckRules.TlsOutcome.OTHER) {
                Logger.w(TAG, "TLS to ${url.substringAfter("://").substringBefore('/')} failed: ${e.javaClass.simpleName}: ${e.message}")
            }
            outcome to false
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun isIntercepted(chain: Array<Certificate>): Boolean {
        val certs = chain.filterIsInstance<X509Certificate>()
        if (certs.isEmpty()) return false
        val top = certs.last()
        return NetCheckRules.looksIntercepted(
            certs.flatMap { listOf(it.subjectX500Principal.name, it.issuerX500Principal.name) },
            top.issuerX500Principal.getName(X500Principal.CANONICAL),
            top.subjectX500Principal.getName(X500Principal.CANONICAL),
            userCaSubjects()
        )
    }

    /** Subjects of the CA certificates the user installed (Settings > Security > Encryption & credentials). */
    private fun userCaSubjects(): Set<String> = runCatching {
        val store = KeyStore.getInstance("AndroidCAStore").apply { load(null) }
        store.aliases().toList()
            .filter { it.startsWith("user:") }
            .mapNotNull { (store.getCertificate(it) as? X509Certificate)?.subjectX500Principal?.getName(X500Principal.CANONICAL) }
            .toSet()
    }.getOrDefault(emptySet())

    private fun installedBlockers(pm: PackageManager): List<String> =
        NetCheckRules.BLOCKER_APPS
            .filter { (pkg, _) -> isInstalled(pm, pkg) }
            .map { it.second }
            .distinct()

    private fun isInstalled(pm: PackageManager, pkg: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        pm.getApplicationInfo(pkg, 0).enabled
    }.getOrDefault(false)

    /**
     * Marks the result stale when the default network changes (a VPN coming up or going away) or
     * its DNS changes (Private DNS), so the next [snapshot] checks again instead of waiting a minute.
     */
    private fun watchNetwork(app: Context) {
        if (!watching.compareAndSet(false, true)) return
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            private var current: Network? = null
            private var dnsKey: String? = null

            override fun onAvailable(network: Network) {
                if (current != null && current != network) changed("network")
                if (current != network) dnsKey = null
                current = network
            }

            override fun onLost(network: Network) {
                if (network == current) {
                    current = null
                    dnsKey = null
                    changed("network lost")
                }
            }

            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) {
                val key = "${lp.isPrivateDnsActive}|${lp.privateDnsServerName}|${lp.dnsServers}"
                if (dnsKey != null && key != dnsKey) changed("dns")
                dnsKey = key
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
            .onFailure { Logger.w(TAG, "can't watch the network: ${it.javaClass.simpleName}") }
    }

    private fun changed(why: String) {
        changedAt = System.currentTimeMillis()
        dirty = true
        Logger.d(TAG, "$why changed, will check again")
    }

    private fun daemonPool(name: String, threads: Int) =
        ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS, LinkedBlockingQueue()) { r ->
            Thread(r, name).apply { isDaemon = true }
        }.apply { allowCoreThreadTimeOut(true) }
}

/** One NetCheck result. [ok] is false when Spotify hosts are blocked or TLS to Spotify fails. */
internal data class NetStatus(
    val vpn: Boolean,
    val privateDns: String,
    val blockers: List<String>,
    val blocked: List<String>,
    val tls: Boolean,
    val intercepted: Boolean,
    val checkedAt: Long
) {
    /** A VPN or blocker app that is merely there is not a failure by itself. */
    val ok: Boolean get() = NetCheckRules.ok(blocked, tls)

    companion object {
        /** Before the first check: nothing known, nothing wrong. */
        val NONE = NetStatus(false, "", emptyList(), emptyList(), tls = false, intercepted = false, checkedAt = 0L)
    }
}

/** The parts of NetCheck that don't touch Android, kept apart so JVM tests can cover them. */
internal object NetCheckRules {

    /** What the Connect server needs: device registration, the dealer socket, the player API, audio and art. */
    val SPOTIFY_HOSTS = listOf(
        "apresolve.spotify.com",
        "dealer.spotify.com",
        "spclient.wg.spotify.com",
        "api-partner.spotify.com",
        "open.spotify.com",
        "audio-ak-spotify-com.akamaized.net",
        "i.scdn.co"
    )

    /** Must resolve for a failed Spotify lookup to count as blocking rather than "no internet". */
    const val CONTROL_HOST = "www.google.com"

    /**
     * An ad server every blocker blocks. Blockers that answer with their own "blocked" address
     * (a block page or the filter's LAN address) give a blocked Spotify host that same address.
     */
    const val AD_CANARY_HOST = "pagead2.googlesyndication.com"

    /** Host to URL for the TLS probe, first one that resolved cleanly is used. */
    val TLS_PROBES = listOf(
        "apresolve.spotify.com" to "https://apresolve.spotify.com/?type=dealer",
        "spclient.wg.spotify.com" to "https://spclient.wg.spotify.com/",
        "open.spotify.com" to "https://open.spotify.com/robots.txt"
    )

    /** Package name to the name the user knows. Same name = same app (shown once). */
    val BLOCKER_APPS = listOf(
        "com.adguard.android" to "AdGuard",
        "com.adguard.android.contentblocker" to "AdGuard",
        "com.adguard.vpn" to "AdGuard VPN",
        "com.celzero.bravedns" to "RethinkDNS",
        "eu.faircode.netguard" to "NetGuard",
        "org.blokada.origin.alarm" to "Blokada",
        "org.blokada.sex" to "Blokada",
        "org.blokada.fem.fdroid" to "Blokada",
        "org.blokada.alarm" to "Blokada",
        "org.blokada.family" to "Blokada",
        "org.adaway" to "AdAway",
        "org.jak_linux.dns66" to "DNS66",
        "com.frostnerd.smokescreen" to "Nebulo",
        "dnsfilter.android" to "personalDNSfilter",
        "com.cloudflare.onedotonedotone" to "Cloudflare 1.1.1.1",
        "net.kollnig.missioncontrol" to "TrackerControl",
        "net.kollnig.missioncontrol.fdroid" to "TrackerControl"
    )

    enum class Problem { APP, PRIVATE_DNS, GENERIC }

    enum class TlsOutcome {
        /** Got an HTTP answer. */
        OK,
        /** The certificate wasn't trusted or didn't match: something is in the middle. */
        CERT,
        /** The handshake broke off. */
        HANDSHAKE,
        /** Timeout, refused, unreachable: not a TLS problem. */
        OTHER
    }

    /** An address no real Spotify server has: 0.0.0.0/8, 127.0.0.0/8, ::, ::1, or those mapped into IPv6. */
    fun isSinkhole(addr: ByteArray): Boolean = when (addr.size) {
        4 -> addr[0].toInt() == 0 || addr[0].toInt() == 127
        16 -> {
            val head = addr.copyOfRange(0, 10)
            val mapped = head.all { it.toInt() == 0 } && addr[10] == 0xff.toByte() && addr[11] == 0xff.toByte()
            when {
                mapped -> isSinkhole(addr.copyOfRange(12, 16))
                addr.copyOfRange(0, 15).all { it.toInt() == 0 } -> addr[15].toInt() == 0 || addr[15].toInt() == 1
                else -> false
            }
        }
        else -> false
    }

    /** DNS answers at all: the control name resolved to real addresses. */
    fun dnsWorks(control: List<ByteArray>?): Boolean =
        !control.isNullOrEmpty() && control.none { isSinkhole(it) }

    /**
     * One Spotify host is blocked when it didn't resolve, resolved to a sinkhole address, or got
     * nothing but the addresses the blocker gave the ad canary.
     */
    fun isBlockedAnswer(addresses: List<ByteArray>?, canary: List<ByteArray>?): Boolean {
        if (addresses.isNullOrEmpty()) return true
        if (addresses.any { isSinkhole(it) }) return true
        if (!canary.isNullOrEmpty() && addresses.all { a -> canary.any { it.contentEquals(a) } }) return true
        return false
    }

    /**
     * The Spotify hosts that are blocked, in the order given. Empty when DNS doesn't work at all
     * (plain "no internet" isn't blocking). A host missing from [answers] (lookup timed out) is
     * not counted.
     */
    fun blockedHosts(
        answers: Map<String, List<ByteArray>?>,
        control: List<ByteArray>?,
        canary: List<ByteArray>?
    ): List<String> {
        if (!dnsWorks(control)) return emptyList()
        return answers.filter { (_, addrs) -> isBlockedAnswer(addrs, canary) }.keys.toList()
    }

    /** How a failed HTTPS request failed. */
    fun tlsOutcome(error: Throwable): TlsOutcome {
        var c: Throwable? = error
        var depth = 0
        while (c != null && depth++ < 10) {
            if (c is CertificateException || c is CertPathValidatorException || c is SSLPeerUnverifiedException) {
                return TlsOutcome.CERT
            }
            val cause = c.cause
            c = if (cause === c) null else cause
        }
        return if (error is SSLHandshakeException || error is SSLProtocolException) TlsOutcome.HANDSHAKE else TlsOutcome.OTHER
    }

    /** A bad certificate, or every probe broke off in the handshake. */
    fun tlsProblem(outcomes: List<TlsOutcome>): Boolean =
        outcomes.any { it == TlsOutcome.CERT } ||
            (outcomes.isNotEmpty() && outcomes.all { it == TlsOutcome.HANDSHAKE })

    /**
     * True when the certificate chain Spotify's host sent was made by something on the phone:
     * AdGuard's CA, or any CA the user installed. Then that app decrypts and filters the traffic.
     */
    fun looksIntercepted(chainNames: List<String>, topIssuer: String, topSubject: String, userCaSubjects: Set<String>): Boolean =
        chainNames.any { it.contains("adguard", ignoreCase = true) } ||
            topIssuer in userCaSubjects || topSubject in userCaSubjects

    fun ok(blocked: List<String>, tls: Boolean): Boolean = blocked.isEmpty() && !tls

    /** Which notification line fits, or null when nothing is wrong. */
    fun problem(status: NetStatus): Problem? = when {
        status.ok -> null
        // DNS-level blocking with Private DNS set and no VPN in the way: the DNS server is the cause.
        !status.vpn && status.privateDns.isNotEmpty() && status.blocked.isNotEmpty() && !status.tls -> Problem.PRIVATE_DNS
        status.blockers.size == 1 -> Problem.APP
        else -> Problem.GENERIC
    }

    fun toJson(status: NetStatus, checking: Boolean): String = JSONObject().apply {
        put("vpn", status.vpn)
        put("privateDns", status.privateDns)
        put("blockers", JSONArray(status.blockers))
        put("blocked", JSONArray(status.blocked))
        put("tls", status.tls)
        put("intercepted", status.intercepted)
        put("ok", status.ok)
        put("checkedAt", status.checkedAt)
        put("checking", checking)
    }.toString()
}
