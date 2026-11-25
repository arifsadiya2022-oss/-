package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Direct Form II Transposed Biquad IIR filter for real-time audio EQ,
 * bass shelving, formant peaking, and tactical radio bandpass shaping.
 */
class BiquadFilter {
    private var b0 = 1.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f

    private var z1 = 0.0f
    private var z2 = 0.0f

    fun reset() {
        z1 = 0.0f
        z2 = 0.0f
    }

    fun process(input: Float): Float {
        val out = b0 * input + z1
        z1 = b1 * input - a1 * out + z2
        z2 = b2 * input - a2 * out
        return out
    }

    fun configureLowShelf(sampleRate: Float, freqHz: Float, gainDb: Float, slope: Float = 0.85f) {
        val a = 10.0.pow(gainDb / 40.0).toFloat()
        val w0 = (2.0 * PI * freqHz.coerceIn(40f, sampleRate * 0.42f) / sampleRate).toFloat()
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / 2.0f * sqrt((a + 1.0f / a) * (1.0f / slope - 1.0f) + 2.0f).coerceAtLeast(0.01f)
        val twoSqrtAAlpha = 2.0f * sqrt(a) * alpha

        val a0 = (a + 1.0f) + (a - 1.0f) * cosW0 + twoSqrtAAlpha
        b0 = (a * ((a + 1.0f) - (a - 1.0f) * cosW0 + twoSqrtAAlpha)) / a0
        b1 = (2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosW0)) / a0
        b2 = (a * ((a + 1.0f) - (a - 1.0f) * cosW0 - twoSqrtAAlpha)) / a0
        a1 = (-2.0f * ((a - 1.0f) + (a + 1.0f) * cosW0)) / a0
        a2 = ((a + 1.0f) + (a - 1.0f) * cosW0 - twoSqrtAAlpha) / a0
    }

    fun configurePeakingEq(sampleRate: Float, freqHz: Float, q: Float, gainDb: Float) {
        val a = 10.0.pow(gainDb / 40.0).toFloat()
        val w0 = (2.0 * PI * freqHz.coerceIn(60f, sampleRate * 0.45f) / sampleRate).toFloat()
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2.0f * q.coerceAtLeast(0.2f))

        val a0 = 1.0f + alpha / a
        b0 = (1.0f + alpha * a) / a0
        b1 = (-2.0f * cosW0) / a0
        b2 = (1.0f - alpha * a) / a0
        a1 = (-2.0f * cosW0) / a0
        a2 = (1.0f - alpha / a) / a0
    }

    fun configureLowPass(sampleRate: Float, cutoffHz: Float, q: Float = 0.707f) {
        val w0 = (2.0 * PI * cutoffHz.coerceIn(120f, sampleRate * 0.45f) / sampleRate).toFloat()
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2.0f * q.coerceAtLeast(0.2f))

        val a0 = 1.0f + alpha
        b0 = ((1.0f - cosW0) * 0.5f) / a0
        b1 = (1.0f - cosW0) / a0
        b2 = ((1.0f - cosW0) * 0.5f) / a0
        a1 = (-2.0f * cosW0) / a0
        a2 = (1.0f - alpha) / a0
    }

    fun configureBandPass(sampleRate: Float, centerHz: Float, q: Float = 1.4f) {
        val w0 = (2.0 * PI * centerHz.coerceIn(150f, sampleRate * 0.42f) / sampleRate).toFloat()
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2.0f * q.coerceAtLeast(0.3f))

        val a0 = 1.0f + alpha
        b0 = alpha / a0
        b1 = 0.0f
        b2 = -alpha / a0
        a1 = (-2.0f * cosW0) / a0
        a2 = (1.0f - alpha) / a0
    }
}
