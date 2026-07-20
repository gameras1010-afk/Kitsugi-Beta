package com.kitsugi.animelist.core.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

class GainAudioProcessor : BaseAudioProcessor() {

    private var gainDb = 0f
    private var linearGain = 1f

    fun setGainDb(db: Float) {
        if (gainDb != db) {
            gainDb = db
            linearGain = Math.pow(10.0, db.toDouble() / 20.0).toFloat()
        }
    }

    fun getGainDb(): Float = gainDb

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        return when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT,
            C.ENCODING_PCM_FLOAT,
            C.ENCODING_PCM_24BIT,
            C.ENCODING_PCM_32BIT,
            C.ENCODING_PCM_8BIT -> inputAudioFormat
            else -> AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)

        if (linearGain == 1f) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        outputBuffer.order(inputBuffer.order())

        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> {
                val shortsCount = remaining / 2
                for (i in 0 until shortsCount) {
                    val sample = inputBuffer.short
                    val boosted = (sample * linearGain).toInt()
                    val clamped = boosted.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    outputBuffer.putShort(clamped)
                }
            }
            C.ENCODING_PCM_FLOAT -> {
                val floatsCount = remaining / 4
                for (i in 0 until floatsCount) {
                    val sample = inputBuffer.float
                    val boosted = sample * linearGain
                    val clamped = boosted.coerceIn(-1.0f, 1.0f)
                    outputBuffer.putFloat(clamped)
                }
            }
            else -> {
                outputBuffer.put(inputBuffer)
            }
        }
        outputBuffer.flip()
    }
}
