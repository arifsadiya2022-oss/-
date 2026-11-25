package com.example.audio

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * Dual-tap Hann-windowed granular delay-line pitch shifter.
 * Operates sample-by-sample with zero heap allocations inside processSample().
 */
class PitchShifterDsp(sampleRateHz: Int) {

    private val bufferSize = 4096
    private val bufferMask = bufferSize - 1
    private val ringBuffer = FloatArray(bufferSize)

    // ~34ms grain window provides clean speech tracking with minimal comb artifacts
    private val grainSize: Float = (sampleRateHz * 0.034f).coerceIn(512f, 2048f)

    private var writeIndex = 0
    private var phase = 0f

    fun reset() {
        ringBuffer.fill(0f)
        writeIndex = 0
        phase = 0f
    }

    fun processSample(input: Float, pitchRatio: Float): Float {
        ringBuffer[writeIndex] = input

        // Fast bypass when near unity pitch to preserve crisp natural transients
        if (pitchRatio in 0.985f..1.015f) {
            writeIndex = (writeIndex + 1) and bufferMask
            return input
        }

        val clampedPitch = pitchRatio.coerceIn(0.50f, 2.00f)
        val phaseDelta = (1.0f - clampedPitch) / grainSize

        phase += phaseDelta
        if (phase >= 1.0f) phase -= 1.0f
        if (phase < 0.0f) phase += 1.0f

        val phase2 = phase + 0.5f - if (phase + 0.5f >= 1.0f) 1.0f else 0.0f

        val delay1 = phase * grainSize + 4.0f
        val delay2 = phase2 * grainSize + 4.0f

        val sample1 = readInterpolated(delay1)
        val sample2 = readInterpolated(delay2)

        // Complementary squared-sine Hann windows: w1 + w2 == 1.0 identically
        val s1 = sin(PI * phase.toDouble()).toFloat()
        val s2 = sin(PI * phase2.toDouble()).toFloat()
        val w1 = s1 * s1
        val w2 = s2 * s2

        writeIndex = (writeIndex + 1) and bufferMask
        return sample1 * w1 + sample2 * w2
    }

    private fun readInterpolated(delaySamples: Float): Float {
        val readPos = writeIndex.toFloat() - delaySamples
        val idx0Float = floor(readPos)
        val frac = readPos - idx0Float
        val idx0 = (idx0Float.toInt() and bufferMask)
        val idx1 = ((idx0 + 1) and bufferMask)
        val a = ringBuffer[idx0]
        val b = ringBuffer[idx1]
        return a + (b - a) * frac
    }
}
