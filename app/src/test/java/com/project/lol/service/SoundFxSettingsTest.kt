package com.project.lol.service

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundFxSettingsTest {

    private val defaults = SoundFxSettings()

    @Test
    fun defaultsAreOnAndFlat() {
        assertEquals(SoundFxSettings(on = true, boost = 0, bass = 0, treble = 0, surround = 0), defaults)
        assertTrue(defaults.isFlat)
    }

    @Test
    fun parsesAFullObject() {
        val s = SoundFxSettings.parse("""{"on":true,"boost":40,"bass":75,"treble":10,"surround":100}""")
        assertEquals(SoundFxSettings(true, 40, 75, 10, 100), s)
        assertFalse(s.isFlat)
    }

    @Test
    fun missingOrBrokenJsonKeepsTheBase() {
        val base = SoundFxSettings(on = false, boost = 20, bass = 30, treble = 40, surround = 50)
        assertEquals(defaults, SoundFxSettings.parse(null))
        assertEquals(defaults, SoundFxSettings.parse(""))
        assertEquals(base, SoundFxSettings.parse("   ", base))
        assertEquals(base, SoundFxSettings.parse("not json", base))
        assertEquals(base, SoundFxSettings.parse("[1,2,3]", base))
        assertEquals(base, SoundFxSettings.parse("{}", base))
    }

    @Test
    fun partialUpdatesKeepTheOtherValues() {
        val base = SoundFxSettings(on = true, boost = 20, bass = 30, treble = 40, surround = 50)
        assertEquals(base.copy(bass = 90), SoundFxSettings.parse("""{"bass":90}""", base))
        assertEquals(base.copy(on = false), SoundFxSettings.parse("""{"on":false}""", base))
    }

    @Test
    fun levelsAreClampedAndRounded() {
        val s = SoundFxSettings.parse("""{"boost":-5,"bass":150,"treble":40.6,"surround":"70"}""")
        assertEquals(0, s.boost)
        assertEquals(100, s.bass)
        assertEquals(41, s.treble)
        assertEquals(70, s.surround)
        assertEquals(100, SoundFxSettings.parse("""{"boost":1e9}""").boost)
    }

    @Test
    fun unusableValuesKeepTheBase() {
        val base = SoundFxSettings(on = true, boost = 20, bass = 30, treble = 40, surround = 50)
        val s = SoundFxSettings.parse("""{"on":"maybe","boost":true,"bass":"loud","treble":null,"surround":{}}""", base)
        assertEquals(base, s)
        assertNull(SoundFxSettings.clamp(Double.NaN))
        assertNull(SoundFxSettings.clamp(null))
        assertNull(SoundFxSettings.clamp(listOf(1)))
    }

    @Test
    fun onAcceptsBooleansTextAndNumbers() {
        assertEquals(false, SoundFxSettings.flag(false))
        assertEquals(true, SoundFxSettings.flag("TRUE"))
        assertEquals(false, SoundFxSettings.flag(" false "))
        assertEquals(true, SoundFxSettings.flag(1))
        assertEquals(false, SoundFxSettings.flag(0))
        assertNull(SoundFxSettings.flag("yes"))
        assertNull(SoundFxSettings.flag(null))
    }

    @Test
    fun offOrAllZeroIsFlat() {
        assertTrue(SoundFxSettings(on = false, boost = 80, bass = 80).isFlat)
        assertTrue(SoundFxSettings(on = true).isFlat)
        assertFalse(SoundFxSettings(on = true, surround = 1).isFlat)
    }

    @Test
    fun jsonRoundTripsInTheStoredShape() {
        val s = SoundFxSettings(on = false, boost = 1, bass = 2, treble = 3, surround = 4)
        val o = s.toJson()
        assertEquals(setOf("on", "boost", "bass", "treble", "surround"), o.keys().asSequence().toSet())
        assertEquals(false, o.getBoolean("on"))
        assertEquals(3, o.getInt("treble"))
        assertEquals(s, SoundFxSettings.parse(o.toString()))
        assertEquals(s, SoundFxSettings.parse(JSONObject(o.toString()).toString(), SoundFxSettings(boost = 99)))
    }

    @Test
    fun boostAndStrengthMappings() {
        assertEquals(0, SoundFxSettings.boostMillibels(0))
        assertEquals(500, SoundFxSettings.boostMillibels(50))
        assertEquals(1000, SoundFxSettings.boostMillibels(100))
        assertEquals(1000, SoundFxSettings.boostMillibels(500))
        assertEquals(0.toShort(), SoundFxSettings.strength(-3))
        assertEquals(350.toShort(), SoundFxSettings.strength(35))
        assertEquals(1000.toShort(), SoundFxSettings.strength(100))
    }

    @Test
    fun equalizerLiftsOnlyTheRightBands() {
        // The stock Android equalizer: 60, 230, 910, 3600 and 14000 Hz, -15..+15 dB.
        fun eq(hz: Double, bass: Int, treble: Int) = SoundFxSettings.eqMillibels(hz, bass, treble, -1500, 1500)

        assertEquals(0, eq(60.0, 0, 0))
        assertEquals(0, eq(14000.0, 0, 0))

        assertEquals(800, eq(60.0, 100, 0))
        assertTrue(eq(230.0, 100, 0) in 1..799)
        assertEquals(0, eq(910.0, 100, 0))
        assertEquals(0, eq(14000.0, 100, 0))

        assertEquals(0, eq(60.0, 0, 100))
        assertEquals(0, eq(910.0, 0, 100))
        assertTrue(eq(3600.0, 0, 100) in 1..599)
        assertEquals(600, eq(14000.0, 0, 100))

        assertEquals(400, eq(60.0, 50, 0))
        // Kept inside a narrow equalizer's range, and nonsense frequencies do nothing.
        assertEquals(300, SoundFxSettings.eqMillibels(60.0, 100, 0, -300, 300))
        assertEquals(0, SoundFxSettings.eqMillibels(0.0, 100, 100, -1500, 1500))
        assertEquals(0, SoundFxSettings.eqMillibels(Double.NaN, 100, 100, -1500, 1500))
    }
}
