package com.example

import com.example.audio.BiquadFilter
import com.example.audio.PitchShifterDsp
import com.example.data.model.VoicePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class DspEngineUnitTest {

    @Test
    fun `pitch shifter produces bounded finite samples across all presets`() {
        val sampleRate = 44100
        val shifter = PitchShifterDsp(sampleRate)

        VoicePreset.entries.forEach { preset ->
            shifter.reset()
            var maxAbs = 0f
            for (i in 0 until 2048) {
                val input = sin(2.0 * PI * 220.0 * i / sampleRate).toFloat() * 0.7f
                val out = shifter.processSample(input, preset.pitchRatio)
                assertFalse("Output must not be NaN for ${preset.id}", out.isNaN())
                assertFalse("Output must not be Infinite for ${preset.id}", out.isInfinite())
                if (abs(out) > maxAbs) maxAbs = abs(out)
            }
            assertTrue("Output should stay bounded for ${preset.id}", maxAbs <= 1.1f)
            assertTrue("Output should remain audible for ${preset.id}", maxAbs > 0.1f)
        }
    }

    @Test
    fun `biquad filters stay stable under shelf peaking and bandpass configurations`() {
        val filter = BiquadFilter()
        filter.configureLowShelf(44100f, 155f, 11.5f)
        for (i in 0 until 1024) {
            val x = sin(2.0 * PI * 100.0 * i / 44100.0).toFloat() * 0.5f
            val y = filter.process(x)
            assertFalse(y.isNaN())
            assertTrue(abs(y) < 5.0f)
        }

        filter.reset()
        filter.configureBandPass(44100f, 1450f, 1.25f)
        for (i in 0 until 1024) {
            val x = sin(2.0 * PI * 1450.0 * i / 44100.0).toFloat() * 0.5f
            val y = filter.process(x)
            assertFalse(y.isNaN())
            assertTrue(abs(y) < 5.0f)
        }
    }

    @Test
    fun `all eight required voice presets exist`() {
        assertEquals(8, VoicePreset.entries.size)
        assertEquals(VoicePreset.DEEP_MALE, VoicePreset.fromId("deep_male"))
        assertEquals(VoicePreset.RADIO_VOICE, VoicePreset.fromId("radio_voice"))
        assertEquals(VoicePreset.CUSTOM_VOICE, VoicePreset.fromId("custom_voice"))
    }
}
