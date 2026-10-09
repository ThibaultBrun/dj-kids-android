package fr.tbrun.djkids.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * EQ 3 bandes (grave/médium/aigu) + filtre balayage, en PCM 16 bits.
 * Une instance par platine (branchée dans la chaîne d'AudioProcessors d'ExoPlayer).
 *
 * Gains réglés en 0..1 (0.5 = neutre). En dessous de ~0.03 la bande est "killée" (coupée),
 * comportement attendu d'une table de mixage DJ.
 */
class ThreeBandEqProcessor : BaseAudioProcessor() {

    @Volatile private var lowN = 0.5f
    @Volatile private var midN = 0.5f
    @Volatile private var highN = 0.5f
    @Volatile private var dirty = true

    private var sampleRate = 44100
    private var channels = 2

    private val low = BiquadCoeffs()
    private val mid = BiquadCoeffs()
    private val high = BiquadCoeffs()

    private var lowState = Array(2) { BiquadState() }
    private var midState = Array(2) { BiquadState() }
    private var highState = Array(2) { BiquadState() }

    fun setLow(v: Float)  { lowN = v.coerceIn(0f, 1f); dirty = true }
    fun setMid(v: Float)  { midN = v.coerceIn(0f, 1f); dirty = true }
    fun setHigh(v: Float) { highN = v.coerceIn(0f, 1f); dirty = true }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channels = inputAudioFormat.channelCount
        lowState = Array(channels) { BiquadState() }
        midState = Array(channels) { BiquadState() }
        highState = Array(channels) { BiquadState() }
        dirty = true
        return inputAudioFormat
    }

    private fun recompute() {
        low.lowShelf(sampleRate, 200.0, toDb(lowN))
        mid.peaking(sampleRate, 1000.0, toDb(midN), q = 0.9)
        high.highShelf(sampleRate, 3200.0, toDb(highN))
        dirty = false
    }

    /** 0.5 -> 0 dB ; 1 -> +6 dB ; 0 -> -40 dB (quasi kill). */
    private fun toDb(v: Float): Double {
        if (v <= 0.03f) return -60.0
        return if (v >= 0.5f) (v - 0.5f) / 0.5f * 6.0
        else (v - 0.5f) / 0.5f * 40.0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (dirty) recompute()
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size)
        val inShorts = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val outShorts = out.order(ByteOrder.nativeOrder()).asShortBuffer()
        val frames = inShorts.remaining() / channels
        var idx = 0
        for (f in 0 until frames) {
            for (c in 0 until channels) {
                val x = inShorts.get(idx) / 32768f
                var y = lowState[c].process(x, low)
                y = midState[c].process(y, mid)
                y = highState[c].process(y, high)
                val s = (y * 32768f).coerceIn(-32768f, 32767f)
                outShorts.put(idx, s.toInt().toShort())
                idx++
            }
        }
        inputBuffer.position(inputBuffer.limit())
        out.position(size)
        out.flip()
    }

    override fun onFlush() {
        lowState.forEach { it.reset() }
        midState.forEach { it.reset() }
        highState.forEach { it.reset() }
    }
}
