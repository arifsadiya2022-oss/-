package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import com.example.data.model.VoiceEngineState
import com.example.data.model.VoicePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Local real-time Android AudioRecord -> Kotlin DSP -> AudioTrack processor.
 * Never records to disk or uploads audio; operates 100% locally in memory.
 */
class VoiceChangerEngine private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioJob: Job? = null

    private val _state = MutableStateFlow(VoiceEngineState())
    val state: StateFlow<VoiceEngineState> = _state.asStateFlow()

    @Volatile private var activePreset: VoicePreset = VoicePreset.DEEP_MALE
    @Volatile private var pitchRatio: Float = VoicePreset.DEEP_MALE.pitchRatio
    @Volatile private var bassDb: Float = VoicePreset.DEEP_MALE.bassDb
    @Volatile private var intensity: Float = VoicePreset.DEEP_MALE.intensity
    @Volatile private var micSensitivity: Float = 1.15f
    @Volatile private var liveMonitoring: Boolean = true
    @Volatile private var clippingProtection: Boolean = true
    @Volatile private var noiseSuppressionRequested: Boolean = true
    @Volatile private var sampleRateHz: Int = 44100
    @Volatile private var filtersDirty: Boolean = true

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun selectPreset(preset: VoicePreset) {
        activePreset = preset
        if (preset != VoicePreset.CUSTOM_VOICE) {
            pitchRatio = preset.pitchRatio
            bassDb = preset.bassDb
            intensity = preset.intensity
        }
        filtersDirty = true
        _state.update {
            it.copy(
                activePreset = preset,
                pitchRatio = pitchRatio,
                bassDb = bassDb,
                intensity = intensity
            )
        }
    }

    fun setPitchRatio(value: Float) {
        val clamped = value.coerceIn(0.55f, 1.85f)
        pitchRatio = clamped
        activePreset = VoicePreset.CUSTOM_VOICE
        filtersDirty = true
        _state.update { it.copy(activePreset = VoicePreset.CUSTOM_VOICE, pitchRatio = clamped) }
    }

    fun setBassDb(value: Float) {
        val clamped = value.coerceIn(-6f, 14f)
        bassDb = clamped
        activePreset = VoicePreset.CUSTOM_VOICE
        filtersDirty = true
        _state.update { it.copy(activePreset = VoicePreset.CUSTOM_VOICE, bassDb = clamped) }
    }

    fun setIntensity(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        intensity = clamped
        _state.update { it.copy(intensity = clamped) }
    }

    fun setMicSensitivity(value: Float) {
        val clamped = value.coerceIn(0.4f, 2.5f)
        micSensitivity = clamped
        _state.update { it.copy(micSensitivity = clamped) }
    }

    fun setLiveMonitoring(enabled: Boolean) {
        liveMonitoring = enabled
        _state.update { it.copy(liveMonitoring = enabled) }
    }

    fun setClippingProtection(enabled: Boolean) {
        clippingProtection = enabled
        _state.update { it.copy(clippingProtection = enabled) }
    }

    fun setNoiseSuppressionEnabled(enabled: Boolean) {
        noiseSuppressionRequested = enabled
        _state.update { it.copy(noiseSuppressionEnabled = enabled) }
    }

    fun updateSampleRate(rateHz: Int) {
        if (sampleRateHz == rateHz) return
        val wasRunning = _state.value.isRunning
        sampleRateHz = rateHz
        _state.update { it.copy(sampleRateHz = rateHz) }
        if (wasRunning) {
            stop()
            start()
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (_state.value.isRunning) return true
        if (!hasRecordPermission()) {
            _state.update {
                it.copy(
                    isRunning = false,
                    errorMessage = "Microphone permission is required for real-time voice processing."
                )
            }
            return false
        }

        _state.update { it.copy(errorMessage = null) }

        audioJob = scope.launch {
            runDspLoop()
        }
        return true
    }

    fun stop() {
        audioJob?.cancel()
        audioJob = null
        _state.update {
            it.copy(
                isRunning = false,
                hardwareNoiseSuppressorActive = false,
                micLevelDb = -60f,
                normalizedLevel = 0f,
                waveformBins = List(16) { 0f },
                isClippingDetected = false,
                latencyMs = 0
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun CoroutineScope.runDspLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val rate = sampleRateHz
        val minRecBuf = AudioRecord.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val minPlayBuf = AudioTrack.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minRecBuf <= 0 || minPlayBuf <= 0) {
            _state.update {
                it.copy(
                    isRunning = false,
                    errorMessage = "Audio hardware rejected $rate Hz mono PCM configuration."
                )
            }
            return
        }

        val frameSamples = (rate / 50).coerceIn(256, 1024) // ~20ms DSP frame
        val recBufferSize = maxOf(minRecBuf, frameSamples * 4)
        val playBufferSize = maxOf(minPlayBuf, frameSamples * 4)
        val estimatedLatencyMs = ((frameSamples * 2f) / rate * 1000f).toInt().coerceAtLeast(15)

        var audioRecord: AudioRecord? = null
        var audioTrack: AudioTrack? = null
        var noiseSuppressor: NoiseSuppressor? = null
        var echoCanceler: AcousticEchoCanceler? = null

        try {
            val source = MediaRecorder.AudioSource.VOICE_COMMUNICATION
            audioRecord = try {
                AudioRecord(
                    source,
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    recBufferSize
                ).also {
                    if (it.state != AudioRecord.STATE_INITIALIZED) {
                        it.release()
                        throw IllegalStateException("VOICE_COMMUNICATION source uninitialized")
                    }
                }
            } catch (_: Exception) {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    recBufferSize
                )
            }

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                _state.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Could not initialize microphone. Another app may be locking audio input."
                    )
                }
                return
            }

            val trackBuilder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(playBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }

            audioTrack = trackBuilder.build()
            if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                _state.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Could not initialize local AudioTrack playback."
                    )
                }
                return
            }

            var nsActive = false
            if (noiseSuppressionRequested && NoiseSuppressor.isAvailable()) {
                noiseSuppressor = runCatching {
                    NoiseSuppressor.create(audioRecord.audioSessionId)?.apply {
                        enabled = true
                        nsActive = enabled
                    }
                }.getOrNull()
            }
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = runCatching {
                    AcousticEchoCanceler.create(audioRecord.audioSessionId)?.apply { enabled = true }
                }.getOrNull()
            }

            audioRecord.startRecording()
            if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                _state.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = "Microphone is busy or blocked by another active call/app."
                    )
                }
                return
            }
            audioTrack.play()

            _state.update {
                it.copy(
                    isRunning = true,
                    hardwareNoiseSuppressorActive = nsActive,
                    latencyMs = estimatedLatencyMs,
                    errorMessage = null
                )
            }

            val inShorts = ShortArray(frameSamples)
            val outShorts = ShortArray(frameSamples)
            val pitchShifter = PitchShifterDsp(rate)
            val bassShelf = BiquadFilter()
            val formantEq = BiquadFilter()
            val toneFilter = BiquadFilter()
            val radioBandPass = BiquadFilter()

            val combDelayLen = (rate * 0.006f).toInt().coerceIn(32, 512)
            val combBuffer = FloatArray(combDelayLen)
            var combIdx = 0
            var ringPhase = 0.0
            var subSign = 1f
            var prevSample = 0f
            var gateGain = 0f
            var meterFrameCounter = 0
            var lastClipTimestamp = 0L

            filtersDirty = true

            while (isActive) {
                val readCount = audioRecord.read(inShorts, 0, frameSamples)
                if (readCount < 0) {
                    _state.update {
                        it.copy(
                            isRunning = false,
                            errorMessage = "Audio input interrupted (code $readCount). Microphone conflict detected."
                        )
                    }
                    break
                }
                if (readCount == 0) continue

                if (filtersDirty) {
                    filtersDirty = false
                    val preset = activePreset
                    bassShelf.configureLowShelf(rate.toFloat(), 155f, bassDb)
                    val formantGain = when (preset) {
                        VoicePreset.FEMALE_STYLE, VoicePreset.CHIPMUNK -> 5.0f
                        VoicePreset.DEEP_MALE, VoicePreset.MONSTER -> -3.5f
                        VoicePreset.ROBOT -> 4.0f
                        else -> 1.5f
                    }
                    val formantFreq = when (preset) {
                        VoicePreset.FEMALE_STYLE, VoicePreset.CHIPMUNK -> 2400f
                        VoicePreset.DEEP_MALE, VoicePreset.HEAVY_BASS -> 650f
                        else -> 1250f
                    }
                    formantEq.configurePeakingEq(rate.toFloat(), formantFreq, 1.1f, formantGain)
                    toneFilter.configureLowPass(rate.toFloat(), preset.formantCutoffHz, 0.75f)
                    radioBandPass.configureBandPass(rate.toFloat(), 1450f, 1.25f)
                }

                val preset = activePreset
                val curPitch = pitchRatio
                val wet = intensity
                val dry = 1.0f - wet
                val sens = micSensitivity
                val useLimiter = clippingProtection
                val monitor = liveMonitoring

                var sumSquares = 0.0
                var frameClipped = false

                // Compute input RMS first for soft noise gate & metering
                for (i in 0 until readCount) {
                    val s = (inShorts[i] / 32768f) * sens
                    sumSquares += (s * s)
                }
                val rms = sqrt(sumSquares / readCount).toFloat()
                val targetGate = if (rms < 0.008f) 0.15f else 1.0f

                for (i in 0 until readCount) {
                    gateGain += (targetGate - gateGain) * 0.02f
                    val drySample = ((inShorts[i] / 32768f) * sens * gateGain).coerceIn(-1.5f, 1.5f)

                    // 1. Pitch shift
                    var wetSample = pitchShifter.processSample(drySample, curPitch)

                    // 2. Preset-specific character DSP
                    when (preset) {
                        VoicePreset.ROBOT -> {
                            ringPhase += (2.0 * PI * 76.0) / rate
                            if (ringPhase > 2.0 * PI) ringPhase -= 2.0 * PI
                            val carrier = sin(ringPhase).toFloat()
                            val delayed = combBuffer[combIdx]
                            val modulated = wetSample * (0.45f + 0.55f * carrier) + delayed * 0.35f
                            combBuffer[combIdx] = modulated
                            combIdx = (combIdx + 1) % combDelayLen
                            wetSample = modulated
                        }
                        VoicePreset.MONSTER -> {
                            // Zero-crossing sub-octave divider + cavern comb
                            if (prevSample <= 0f && drySample > 0f) {
                                subSign = -subSign
                            }
                            prevSample = drySample
                            val subOct = abs(wetSample) * subSign * 0.32f
                            wetSample = (wetSample * 0.85f + subOct)
                        }
                        VoicePreset.RADIO_VOICE -> {
                            val bp = radioBandPass.process(wetSample)
                            wetSample = tanh((bp * 2.2f).toDouble()).toFloat() * 0.9f
                        }
                        else -> Unit
                    }

                    // 3. Bass shelf + Formant EQ + Anti-harshness tone filter
                    wetSample = bassShelf.process(wetSample)
                    wetSample = formantEq.process(wetSample)
                    wetSample = toneFilter.process(wetSample)

                    // 4. Dry / Wet intensity blend
                    var mixed = drySample * dry + wetSample * wet

                    // 5. Clipping protection soft-knee limiter
                    if (abs(mixed) > 0.96f) {
                        frameClipped = true
                    }
                    if (useLimiter) {
                        if (abs(mixed) > 0.78f) {
                            mixed = tanh((mixed * 1.15f).toDouble()).toFloat() * 0.92f
                        }
                        mixed = mixed.coerceIn(-0.98f, 0.98f)
                    } else {
                        mixed = mixed.coerceIn(-1.0f, 1.0f)
                    }

                    outShorts[i] = if (monitor) {
                        (mixed * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    } else {
                        0
                    }
                }

                if (monitor) {
                    audioTrack.write(outShorts, 0, readCount)
                }

                if (frameClipped) {
                    lastClipTimestamp = System.currentTimeMillis()
                }

                meterFrameCounter++
                if (meterFrameCounter >= 2) {
                    meterFrameCounter = 0
                    val db = if (rms > 1e-5f) (20f * log10(rms)).coerceIn(-60f, 0f) else -60f
                    val norm = ((db + 54f) / 54f).coerceIn(0f, 1f)
                    val bins = buildWaveformBins(inShorts, readCount, sens)
                    val clipActive = (System.currentTimeMillis() - lastClipTimestamp) < 450L
                    _state.update {
                        it.copy(
                            micLevelDb = db,
                            normalizedLevel = norm,
                            waveformBins = bins,
                            isClippingDetected = clipActive
                        )
                    }
                }
            }
        } catch (_: SecurityException) {
            _state.update {
                it.copy(
                    isRunning = false,
                    errorMessage = "Microphone permission was revoked."
                )
            }
        } catch (t: Throwable) {
            _state.update {
                it.copy(
                    isRunning = false,
                    errorMessage = t.message ?: "Voice DSP engine encountered an unexpected audio error."
                )
            }
        } finally {
            runCatching { noiseSuppressor?.release() }
            runCatching { echoCanceler?.release() }
            runCatching {
                if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
                audioRecord?.release()
            }
            runCatching {
                if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop()
                }
                audioTrack?.release()
            }
        }
    }

    private fun buildWaveformBins(samples: ShortArray, count: Int, sens: Float): List<Float> {
        val binCount = 16
        val step = (count / binCount).coerceAtLeast(1)
        return List(binCount) { bin ->
            val start = bin * step
            val end = ((bin + 1) * step).coerceAtMost(count)
            var peak = 0f
            for (i in start until end) {
                val amp = abs(samples[i] / 32768f) * sens
                if (amp > peak) peak = amp
            }
            (peak * 1.8f).coerceIn(0.04f, 1.0f)
        }
    }

    companion object {
        @Volatile
        private var instance: VoiceChangerEngine? = null

        fun getInstance(context: Context): VoiceChangerEngine {
            return instance ?: synchronized(this) {
                instance ?: VoiceChangerEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
