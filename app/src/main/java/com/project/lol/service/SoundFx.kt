package com.project.lol.service

import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper
import com.project.lol.util.Logger
import org.json.JSONObject
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Sound controls on the Server screen: volume boost, bass boost, treble and surround.
 *
 * The web player's audio leaves through Chromium inside the WebView and its audio session id
 * isn't exposed, so the effects sit on the global output mix (audio session 0). Session 0 is
 * deprecated but still works on many phones; an effect the phone refuses is reported as
 * unsupported by [stateJson]. **Session-0 effects change all audio on the phone, not only
 * SpotiOS: other apps are boosted too while SpotiOS runs.** So MediaNotificationService calls
 * [apply] when it starts and [release] when it stops, nothing is created while the settings are
 * off or flat, and each effect only exists while its own level is above 0.
 *
 * Settings live in `spotilol_prefs` under [KEY] as
 * `{"on":true,"boost":0,"bass":0,"treble":0,"surround":0}` (levels 0..100, see [SoundFxSettings]).
 * Volume boost uses LoudnessEnhancer (a compressor, so it doesn't hard clip), or a
 * DynamicsProcessing input gain plus limiter where LoudnessEnhancer is missing. Bass uses BassBoost,
 * or the Equalizer's low bands when BassBoost is missing or has no adjustable strength. Treble uses
 * the Equalizer's high bands, surround the Virtualizer.
 */
object SoundFx {

    private const val TAG = "soundfx"
    private const val PREFS = "spotilol_prefs"
    const val KEY = "SpoFx"

    /** The global output mix. */
    private const val SESSION = 0
    /** Normal priority: a new handle at the same priority takes control of a shared effect. */
    private const val PRIORITY = 0

    private enum class Fx { LOUDNESS, DYNAMICS, BASS_BOOST, EQUALIZER, VIRTUALIZER }

    private val main by lazy { Handler(Looper.getMainLooper()) }
    private val resync = Runnable { resyncNow() }

    /** The service is up ([apply] was called and [release] wasn't): effects follow the settings. */
    private var attached = false
    private var appContext: Context? = null

    private var loudness: LoudnessEnhancer? = null
    private var dynamics: DynamicsProcessing? = null
    private var bassBoost: BassBoost? = null
    private var equalizer: Equalizer? = null
    private var virtualizer: Virtualizer? = null

    /** Whether each effect could be created on session 0; tried once per process. */
    private val probes = HashMap<Fx, Boolean>()
    /** BassBoost takes a strength (false: it's on/off only). Read when it's probed. */
    private var bassStrength = false
    /** The Equalizer has a band low enough for bass / high enough for treble. Read when it's probed. */
    private var eqLow = false
    private var eqHigh = false

    /** Starts (or updates) the effects to match the settings. Called by MediaNotificationService.onCreate. */
    @Synchronized
    fun apply(context: Context) {
        appContext = context.applicationContext
        attached = true
        sync(load(context))
    }

    /** Releases every effect, so nothing stays on the output mix. Called by MediaNotificationService.onDestroy. */
    @Synchronized
    fun release() {
        attached = false
        main.removeCallbacks(resync)
        if (anyLive()) Logger.i(TAG, "effects released")
        loudness = free(loudness)
        dynamics = free(dynamics)
        bassBoost = free(bassBoost)
        equalizer = free(equalizer)
        virtualizer = free(virtualizer)
    }

    /**
     * The settings plus what this phone can run, for the Server screen:
     * `{"on":true,"boost":0,"bass":0,"treble":0,"surround":0,
     *   "supported":{"boost":true,"bass":true,"treble":true,"surround":true},"active":false}`.
     * `active` is true while at least one effect is running.
     */
    @Synchronized
    fun stateJson(context: Context): String {
        val o = load(context).toJson()
        o.put("supported", JSONObject()
            .put("boost", supported(Fx.LOUDNESS) || supported(Fx.DYNAMICS))
            .put("bass", supported(Fx.BASS_BOOST) || (supported(Fx.EQUALIZER) && eqLow))
            .put("treble", supported(Fx.EQUALIZER) && eqHigh)
            .put("surround", supported(Fx.VIRTUALIZER)))
        o.put("active", attached && anyLive())
        return o.toString()
    }

    /**
     * Saves new settings from the page: keys it leaves out keep their value, levels are clamped
     * to 0..100. The effects follow on the main thread while the service runs; with no service
     * the settings are only saved, and [apply] picks them up when it starts.
     */
    fun set(context: Context, json: String) {
        val app = context.applicationContext
        synchronized(this) {
            val next = SoundFxSettings.parse(json, load(app))
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY, next.toJson().toString())
                .apply()
            Logger.d(TAG, "set $next")
        }
        main.removeCallbacks(resync)
        main.post(resync)
    }

    private fun load(context: Context): SoundFxSettings =
        SoundFxSettings.parse(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))

    @Synchronized
    private fun resyncNow() {
        val ctx = appContext ?: return
        if (attached) sync(load(ctx))
    }

    private fun sync(s: SoundFxSettings) {
        val boost = if (s.on) s.boost else 0
        val bass = if (s.on) s.bass else 0
        val treble = if (s.on) s.treble else 0
        val surround = if (s.on) s.surround else 0
        val bassOnEq = bass > 0 && !(supported(Fx.BASS_BOOST) && bassStrength) &&
            supported(Fx.EQUALIZER) && eqLow

        // Volume boost: LoudnessEnhancer, else DynamicsProcessing.
        val boostMb = SoundFxSettings.boostMillibels(boost)
        loudness = if (boost > 0 && supported(Fx.LOUDNESS)) {
            drive(loudness, { LoudnessEnhancer(SESSION) }) { it.setTargetGain(boostMb) }
        } else free(loudness)
        dynamics = if (boost > 0 && loudness == null && supported(Fx.DYNAMICS)) {
            drive(dynamics, { newDynamics() }) { it.setInputGainAllChannelsTo(boostMb / 100f) }
        } else free(dynamics)

        bassBoost = if (bass > 0 && !bassOnEq && supported(Fx.BASS_BOOST)) {
            drive(bassBoost, { BassBoost(PRIORITY, SESSION) }) { it.setStrength(SoundFxSettings.strength(bass)) }
        } else free(bassBoost)

        val eqBass = if (bassOnEq) bass else 0
        equalizer = if ((eqBass > 0 || treble > 0) && supported(Fx.EQUALIZER)) {
            drive(equalizer, { Equalizer(PRIORITY, SESSION) }) { eq ->
                val range = eq.bandLevelRange
                for (band in 0 until eq.numberOfBands) {
                    val b = band.toShort()
                    val mb = SoundFxSettings.eqMillibels(
                        eq.getCenterFreq(b) / 1000.0, eqBass, treble, range[0].toInt(), range[1].toInt()
                    )
                    eq.setBandLevel(b, mb.toShort())
                }
            }
        } else free(equalizer)

        virtualizer = if (surround > 0 && supported(Fx.VIRTUALIZER)) {
            drive(virtualizer, { Virtualizer(PRIORITY, SESSION) }) { it.setStrength(SoundFxSettings.strength(surround)) }
        } else free(virtualizer)

        Logger.d(TAG, "applied $s: loudness=${loudness != null} dynamics=${dynamics != null} " +
            "bassBoost=${bassBoost != null} eq=${equalizer != null} virtualizer=${virtualizer != null}")
    }

    /**
     * Sets up [current] (or a new effect from [create]) with [configure] and turns it on. One that
     * fails (audio server restarted, another app took control) is released and made again once.
     */
    private fun <T : AudioEffect> drive(current: T?, create: () -> T, configure: (T) -> Unit): T? {
        var fx = current
        repeat(2) {
            val live = fx ?: try {
                create().also { made ->
                    // Back in control after another app (or a probe) let go: put our levels back.
                    made.setControlStatusListener { _, granted -> if (granted) main.post(resync) }
                }
            } catch (e: Exception) {
                Logger.w(TAG, "effect not created: ${e.javaClass.simpleName} ${e.message}")
                return null
            }
            try {
                configure(live)
                check(live.setEnabled(true) == AudioEffect.SUCCESS) { "not enabled" }
                return live
            } catch (e: Exception) {
                Logger.w(TAG, "${live.javaClass.simpleName} failed: ${e.javaClass.simpleName} ${e.message}")
                free(live)
                fx = null
            }
        }
        return null
    }

    /** Volume boost where LoudnessEnhancer is missing: input gain into a limiter, so it doesn't clip. */
    private fun newDynamics(): DynamicsProcessing {
        val cfg = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION, 2,
            false, 0, false, 0, false, 0, true
        ).setLimiterAllChannelsTo(
            // inUse, enabled, linkGroup, attack ms, release ms, ratio, threshold dB, post gain dB
            DynamicsProcessing.Limiter(true, true, 0, 1f, 60f, 10f, -2f, 0f)
        ).build()
        return DynamicsProcessing(PRIORITY, SESSION, cfg)
    }

    /**
     * Puts the effect back to neutral and switches it off before releasing it: on session 0 the
     * engine can be shared with another app's handle, and it must not keep our levels.
     */
    private fun free(fx: AudioEffect?): Nothing? {
        if (fx == null) return null
        runCatching { fx.setControlStatusListener(null) }
        runCatching {
            when (fx) {
                is LoudnessEnhancer -> fx.setTargetGain(0)
                is DynamicsProcessing -> fx.setInputGainAllChannelsTo(0f)
                is BassBoost -> fx.setStrength(0)
                is Virtualizer -> fx.setStrength(0)
                is Equalizer -> for (b in 0 until fx.numberOfBands) fx.setBandLevel(b.toShort(), 0)
            }
        }
        runCatching { fx.setEnabled(false) }
        runCatching { fx.release() }
        return null
    }

    private fun anyLive() =
        loudness != null || dynamics != null || bassBoost != null || equalizer != null || virtualizer != null

    private fun supported(fx: Fx): Boolean = probes.getOrPut(fx) { probe(fx) }

    /** Creates the effect on session 0 (switched off), reads what's needed, and releases it. */
    private fun probe(fx: Fx): Boolean {
        val made: AudioEffect = try {
            when (fx) {
                Fx.LOUDNESS -> LoudnessEnhancer(SESSION)
                Fx.DYNAMICS -> newDynamics()
                Fx.BASS_BOOST -> BassBoost(PRIORITY, SESSION)
                Fx.EQUALIZER -> Equalizer(PRIORITY, SESSION)
                Fx.VIRTUALIZER -> Virtualizer(PRIORITY, SESSION)
            }
        } catch (e: Exception) {
            Logger.i(TAG, "$fx unsupported on this phone: ${e.javaClass.simpleName} ${e.message}")
            return false
        }
        return try {
            when (made) {
                is BassBoost -> bassStrength = made.strengthSupported
                is Equalizer -> {
                    val hz = (0 until made.numberOfBands).map { made.getCenterFreq(it.toShort()) / 1000.0 }
                    eqLow = hz.any { it < SoundFxSettings.BASS_TOP_HZ }
                    eqHigh = hz.any { it > SoundFxSettings.TREBLE_BOTTOM_HZ }
                    Logger.i(TAG, "equalizer bands: ${hz.joinToString { it.roundToInt().toString() }} Hz")
                }
            }
            Logger.i(TAG, "$fx supported")
            true
        } catch (e: Exception) {
            Logger.i(TAG, "$fx unusable: ${e.javaClass.simpleName} ${e.message}")
            false
        } finally {
            runCatching { made.release() }
        }
    }
}

/**
 * Sound settings from the Server screen, each level 0..100. Plain Kotlin and org.json only, so
 * it can be tested on the JVM; [SoundFx] turns it into audio effects.
 */
data class SoundFxSettings(
    val on: Boolean = true,
    val boost: Int = 0,
    val bass: Int = 0,
    val treble: Int = 0,
    val surround: Int = 0,
) {
    /** Nothing to do: switched off, or every level at 0. */
    val isFlat: Boolean get() = !on || (boost == 0 && bass == 0 && treble == 0 && surround == 0)

    fun toJson(): JSONObject = JSONObject()
        .put("on", on)
        .put("boost", boost)
        .put("bass", bass)
        .put("treble", treble)
        .put("surround", surround)

    companion object {
        const val MAX = 100
        /** LoudnessEnhancer target gain at boost 100: +10 dB. */
        const val MAX_BOOST_MB = 1000
        /** Equalizer lift at bass 100 (only when BassBoost can't do it): +8 dB. */
        const val MAX_EQ_BASS_MB = 800
        /** Equalizer lift at treble 100: +6 dB. */
        const val MAX_TREBLE_MB = 600
        /** Bass lifts bands below this, fully at a quarter of it (100 Hz) and under. */
        const val BASS_TOP_HZ = 400.0
        /** Treble lifts bands above this, fully at four times it (8 kHz) and over. */
        const val TREBLE_BOTTOM_HZ = 2000.0

        /**
         * Reads settings sent by the page (or stored) over [base]: a key that's missing or not
         * usable keeps base's value, levels are clamped to 0..100 and rounded. Text that isn't a
         * JSON object gives [base] unchanged.
         */
        fun parse(json: String?, base: SoundFxSettings = SoundFxSettings()): SoundFxSettings {
            if (json.isNullOrBlank()) return base
            val o = try { JSONObject(json) } catch (_: Exception) { return base }
            return SoundFxSettings(
                on = flag(o.opt("on")) ?: base.on,
                boost = clamp(o.opt("boost")) ?: base.boost,
                bass = clamp(o.opt("bass")) ?: base.bass,
                treble = clamp(o.opt("treble")) ?: base.treble,
                surround = clamp(o.opt("surround")) ?: base.surround,
            )
        }

        /** A level: a number (or numeric text) clamped to 0..100 and rounded; anything else is null. */
        fun clamp(value: Any?): Int? {
            val d = when (value) {
                is Number -> value.toDouble()
                is String -> value.trim().toDoubleOrNull()
                else -> null
            } ?: return null
            if (d.isNaN()) return null
            return d.coerceIn(0.0, MAX.toDouble()).roundToInt()
        }

        /** A switch: a boolean, "true"/"false", or a number (non-zero is on); anything else is null. */
        fun flag(value: Any?): Boolean? = when (value) {
            is Boolean -> value
            is String -> value.trim().lowercase().toBooleanStrictOrNull()
            is Number -> value.toDouble().let { if (it.isNaN()) null else it != 0.0 }
            else -> null
        }

        /** LoudnessEnhancer target gain in millibels for a boost level. */
        fun boostMillibels(level: Int): Int = level.coerceIn(0, MAX) * MAX_BOOST_MB / MAX

        /** BassBoost / Virtualizer strength (0..1000) for a level. */
        fun strength(level: Int): Short = (level.coerceIn(0, MAX) * 10).toShort()

        /**
         * Equalizer level in millibels for a band centred on [hz]: bass lifts the low bands, treble
         * the high ones, smoothly over two octaves, kept inside the equalizer's [minMb]..[maxMb].
         */
        fun eqMillibels(hz: Double, bass: Int, treble: Int, minMb: Int, maxMb: Int): Int {
            if (!(hz > 0.0)) return 0.coerceAtMost(maxMb).coerceAtLeast(minMb)
            val low = weight(log2(BASS_TOP_HZ / hz) / 2.0)
            val high = weight(log2(hz / TREBLE_BOTTOM_HZ) / 2.0)
            val mb = bass.coerceIn(0, MAX) * MAX_EQ_BASS_MB / MAX.toDouble() * low +
                treble.coerceIn(0, MAX) * MAX_TREBLE_MB / MAX.toDouble() * high
            return mb.roundToInt().coerceAtMost(maxMb).coerceAtLeast(minMb)
        }

        private fun weight(x: Double): Double = if (x.isNaN()) 0.0 else x.coerceIn(0.0, 1.0)
    }
}
