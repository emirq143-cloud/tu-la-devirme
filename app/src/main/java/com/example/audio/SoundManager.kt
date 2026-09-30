package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sampleRate = 22050
    var isEnabled: Boolean = true

    // Pentatonic scale frequencies for musical combo scaling
    private val comboFrequencies = floatArrayOf(
        261.63f, 293.66f, 329.63f, 392.00f, 440.00f, // C4, D4, E4, G4, A4
        523.25f, 587.33f, 659.25f, 783.99f, 880.00f, // C5, D5, E5, G5, A5
        1046.50f, 1174.66f, 1318.51f, 1567.98f, 1760.00f, // C6, D6, E6, G6, A6
        2093.00f, 2349.32f, 2637.02f, 3135.96f, 3520.00f  // C7, D7, E7, G7, A7
    )

    fun playBrickHit(comboIndex: Int) {
        if (!isEnabled) return
        scope.launch {
            val freqIndex = comboIndex.coerceIn(0, comboFrequencies.size - 1)
            val freq = comboFrequencies[freqIndex]
            val durationMs = 90
            val samplesCount = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(samplesCount)

            for (i in 0 until samplesCount) {
                val t = i.toFloat() / sampleRate
                // Sine wave with overtone and exponential decay envelope
                val fundamental = sin(2.0 * PI * freq * t)
                val overtone = 0.4 * sin(2.0 * PI * (freq * 2.756) * t)
                val envelope = exp(-35.0 * t) // fast percussive decay
                val sampleVal = ((fundamental + overtone) * envelope * 0.75 * Short.MAX_VALUE).toInt()
                buffer[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playWallBounce() {
        if (!isEnabled) return
        scope.launch {
            val durationMs = 30
            val samplesCount = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(samplesCount)
            val freq = 200f
            for (i in 0 until samplesCount) {
                val t = i.toFloat() / sampleRate
                val envelope = exp(-70.0 * t)
                val sampleVal = (sin(2.0 * PI * freq * t) * envelope * 0.3 * Short.MAX_VALUE).toInt()
                buffer[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playLaserZap() {
        if (!isEnabled) return
        scope.launch {
            val durationMs = 220
            val samplesCount = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(samplesCount)
            for (i in 0 until samplesCount) {
                val progress = i.toFloat() / samplesCount
                val freq = 1600f - progress * 1100f // sweep down
                val t = i.toFloat() / sampleRate
                val envelope = (1f - progress) * (1f - progress)
                val sampleVal = (sin(2.0 * PI * freq * t) * envelope * 0.6 * Short.MAX_VALUE).toInt()
                buffer[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playExplosion() {
        if (!isEnabled) return
        scope.launch {
            val durationMs = 300
            val samplesCount = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(samplesCount)
            for (i in 0 until samplesCount) {
                val progress = i.toFloat() / samplesCount
                val t = i.toFloat() / sampleRate
                val noise = (Math.random() * 2.0 - 1.0)
                val lowBoom = sin(2.0 * PI * (90f - progress * 50f) * t)
                val envelope = exp(-12.0 * progress)
                val sampleVal = ((noise * 0.4 + lowBoom * 0.7) * envelope * 0.8 * Short.MAX_VALUE).toInt()
                buffer[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playBallPickup() {
        if (!isEnabled) return
        scope.launch {
            val durationMs = 120
            val samplesCount = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(samplesCount)
            for (i in 0 until samplesCount) {
                val t = i.toFloat() / sampleRate
                val f1 = 1200f
                val f2 = 1800f
                val envelope = exp(-25.0 * t)
                val sampleVal = ((sin(2.0 * PI * f1 * t) + sin(2.0 * PI * f2 * t)) * 0.4 * envelope * Short.MAX_VALUE).toInt()
                buffer[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playVictoryFanfare() {
        if (!isEnabled) return
        scope.launch {
            val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f)
            val noteDurationMs = 110
            for (freq in notes) {
                val samplesCount = (sampleRate * (noteDurationMs / 1000f)).toInt()
                val buffer = ShortArray(samplesCount)
                for (i in 0 until samplesCount) {
                    val t = i.toFloat() / sampleRate
                    val envelope = exp(-12.0 * t)
                    val sampleVal = (sin(2.0 * PI * freq * t) * envelope * 0.7 * Short.MAX_VALUE).toInt()
                    buffer[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                playPcm(buffer)
                kotlinx.coroutines.delay(90)
            }
        }
    }

    fun playClick() {
        if (!isEnabled) return
        scope.launch {
            val durationMs = 25
            val samplesCount = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(samplesCount)
            for (i in 0 until samplesCount) {
                val t = i.toFloat() / sampleRate
                val envelope = exp(-90.0 * t)
                val sampleVal = (sin(2.0 * PI * 800f * t) * envelope * 0.4 * Short.MAX_VALUE).toInt()
                buffer[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            scope.launch {
                kotlinx.coroutines.delay(buffer.size * 1000L / sampleRate + 50)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
