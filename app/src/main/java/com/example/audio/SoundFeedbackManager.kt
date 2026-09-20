package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * SoundFeedbackManager provides local, asset-free acoustic feedback:
 * 1. Harmonious completion chime upon focus session success.
 * 2. Gentle warning chime when physical placement baseline is violated.
 * 3. Soft synthesized forest breeze / brown-noise ambient sound during focus.
 */
class SoundFeedbackManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var toneGenerator: ToneGenerator? = null
    private var ambientJob: Job? = null
    private var audioTrack: AudioTrack? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        } catch (_: Exception) {
            // ToneGenerator fallback if audio output unavailable
        }
    }

    fun playWarningChime(enabled: Boolean) {
        if (!enabled) return
        try {
            scope.launch(Dispatchers.Default) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
            }
        } catch (_: Exception) {}
    }

    /**
     * Synthesizes a delicate, calm nature-based acoustic chime chord
     * (E major: E5, G#5, B5, E6) with gentle exponential decay,
     * fulfilling the calm forest ambiance requirement without external audio assets.
     */
    fun playCompletionChime(enabled: Boolean) {
        if (!enabled) return
        scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 22050
                val durationSec = 1.4f
                val numSamples = (sampleRate * durationSec).toInt()
                val buffer = ShortArray(numSamples)
                val freqs = floatArrayOf(659.25f, 830.61f, 987.77f, 1318.51f) // E5, G#5, B5, E6
                val amplitudes = floatArrayOf(0.28f, 0.22f, 0.18f, 0.14f)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    val envelope = Math.exp((-2.8 * t)).toFloat() // Smooth exponential fade-out
                    var sampleVal = 0f
                    for (f in freqs.indices) {
                        sampleVal += sin(2.0 * Math.PI * freqs[f] * t).toFloat() * amplitudes[f]
                    }
                    val finalSample = (sampleVal * envelope * 32767f).toInt().coerceIn(-32768, 32767)
                    buffer[i] = finalSample.toShort()
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                val track = AudioTrack(
                    audioAttributes,
                    audioFormat,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                track.write(buffer, 0, buffer.size)
                track.play()
                // Auto release after sound finishes
                kotlinx.coroutines.delay(1600L)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Fallback to ToneGenerator if audio track cannot be allocated
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                } catch (_: Exception) {}
            }
        }
    }

    fun startAmbientSound(enabled: Boolean) {
        if (!enabled) {
            stopAmbientSound()
            return
        }
        if (ambientJob != null && ambientJob?.isActive == true) return

        ambientJob = scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 22050
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = (minBufferSize * 2).coerceAtLeast(sampleRate / 4)

                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                val format = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                val track = AudioTrack(
                    attributes,
                    format,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                audioTrack = track
                track.play()

                val buffer = ShortArray(bufferSize)
                var lastVal = 0.0f

                // Generate gentle low-pass filtered brown noise (resembling soft rustling forest breeze)
                while (isActive) {
                    for (i in buffer.indices) {
                        val white = (Random.nextFloat() * 2f - 1f) * 0.06f // Low gentle volume
                        lastVal = (lastVal * 0.95f) + (white * 0.05f)
                        val sample = (lastVal * 32767f).toInt().coerceIn(-32768, 32767)
                        buffer[i] = sample.toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                }

                track.stop()
                track.release()
            } catch (_: Exception) {
                // Silently finish if audio track unsupported
            }
        }
    }

    fun stopAmbientSound() {
        ambientJob?.cancel()
        ambientJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun release() {
        stopAmbientSound()
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }
}
