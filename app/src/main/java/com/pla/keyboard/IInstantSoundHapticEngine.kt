package com.pla.keyboard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class InstantSoundHapticEngine(
    private val context: Context,
    private val prefs: PrefsManager
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale("id", "ID"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
            tts?.setSpeechRate(1.2f)
            ttsReady = true
        }
    }

    /**
     * Memicu suara ketukan & getaran mikro secara instan (0ms blocking di main thread)
     */
    fun triggerInstantKeyFeedback(normalizedX: Float, isSpecialKey: Boolean) {
        triggerMicroHaptic(isSpecialKey)

        if (!prefs.keySoundEnabled || prefs.soundVolumePercent <= 2) return

        val volume = prefs.soundVolumePercent / 100f
        val profile = prefs.soundProfile
        val useSpatial = prefs.spatialAudioEnabled
        val panX = if (useSpatial) normalizedX.coerceIn(0f, 1f) else 0.5f

        Thread {
            playPrecomputedClick(volume, profile, panX, isSpecialKey)
        }.start()
    }

    fun speakKeyForAccessibility(label: String) {
        if (!ttsReady) return
        val spoken = when (label) {
            " " -> "Spasi"
            "DEL" -> "Hapus"
            "ENTER" -> "Enter"
            "SHIFT" -> "Shift"
            "?123" -> "Simbol dan angka"
            "ABC" -> "Huruf"
            else -> label
        }
        tts?.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "pla_kb_key")
    }

    private fun triggerMicroHaptic(isSpecialKey: Boolean) {
        val strength = prefs.hapticStrengthPercent
        if (strength <= 3 || !vibrator.hasVibrator()) return

        val durationMs = if (isSpecialKey) 18L else 12L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amp = ((strength / 100.0) * 255).toInt().coerceIn(1, 255)
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amp))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun playPrecomputedClick(
        volume: Float,
        profile: Int,
        panX: Float,
        isSpecialKey: Boolean
    ) {
        val sampleRate = 44100
        val durationMs = 24
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples * 2)

        val angle = panX * (PI / 2.0)
        val leftGain = cos(angle).toFloat().coerceIn(0.08f, 1.0f)
        val rightGain = sin(angle).toFloat().coerceIn(0.08f, 1.0f)

        val baseFreq = when (profile) {
            1 -> if (isSpecialKey) 460.0 else 540.0   // Soft Tap
            2 -> if (isSpecialKey) 780.0 else 1080.0  // Mechanical
            else -> if (isSpecialKey) 640.0 else 860.0 // Crisp
        }

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val decay = (1.0 - progress) * (1.0 - progress)
            val t = i.toDouble() / sampleRate
            val wave = sin(2.0 * PI * baseFreq * t) * decay * volume * 0.7

            buffer[i * 2] = (wave * leftGain * Short.MAX_VALUE)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer[i * 2 + 1] = (wave * rightGain * Short.MAX_VALUE)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(28L)
            track.release()
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}