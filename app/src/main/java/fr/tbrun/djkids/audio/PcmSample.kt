package fr.tbrun.djkids.audio

import android.content.Context
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Échantillon audio décodé en mémoire : mono, float [-1,1], prêt à être scrubbé/mixé par
 * [ScratchEngine]. Chargé depuis un WAV PCM (8/16/24/32 bits ou float) des assets.
 */
class PcmSample(
    val data: FloatArray,
    val sampleRate: Int,
) {
    val frames: Int get() = data.size

    companion object {
        /** Charge un WAV mono/stéréo des assets ; downmix mono. Retourne null si illisible. */
        fun fromAsset(context: Context, path: String): PcmSample? = runCatching {
            context.assets.open(path).use { readWav(it) }
        }.getOrNull()

        private fun readWav(input: InputStream): PcmSample {
            val bytes = input.readBytes()
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(bytes.size > 44) { "WAV trop court" }
            require(readTag(bb, 0) == "RIFF" && readTag(bb, 8) == "WAVE") { "pas un WAV" }

            var pos = 12
            var channels = 1
            var sampleRate = 44100
            var bits = 16
            var audioFormat = 1 // 1 = PCM int, 3 = float
            var dataOffset = -1
            var dataLen = 0

            while (pos + 8 <= bytes.size) {
                val id = readTag(bb, pos)
                val size = bb.getInt(pos + 4)
                val body = pos + 8
                when (id) {
                    "fmt " -> {
                        audioFormat = bb.getShort(body).toInt() and 0xFFFF
                        channels = bb.getShort(body + 2).toInt() and 0xFFFF
                        sampleRate = bb.getInt(body + 4)
                        bits = bb.getShort(body + 14).toInt() and 0xFFFF
                    }
                    "data" -> { dataOffset = body; dataLen = size }
                }
                pos = body + size + (size and 1) // chunks alignés sur 2 octets
                if (dataOffset >= 0 && id == "data") break
            }
            require(dataOffset >= 0) { "chunk data absent" }
            dataLen = minOf(dataLen, bytes.size - dataOffset)

            val bytesPerSample = bits / 8
            val frameCount = dataLen / (bytesPerSample * channels)
            val out = FloatArray(frameCount)
            var p = dataOffset
            for (f in 0 until frameCount) {
                var acc = 0f
                for (c in 0 until channels) {
                    acc += readSample(bb, p, bits, audioFormat)
                    p += bytesPerSample
                }
                out[f] = acc / channels
            }
            return PcmSample(out, sampleRate)
        }

        private fun readSample(bb: ByteBuffer, p: Int, bits: Int, fmt: Int): Float = when {
            fmt == 3 && bits == 32 -> bb.getFloat(p)
            bits == 16 -> bb.getShort(p) / 32768f
            bits == 8 -> ((bb.get(p).toInt() and 0xFF) - 128) / 128f
            bits == 24 -> {
                val b0 = bb.get(p).toInt() and 0xFF
                val b1 = bb.get(p + 1).toInt() and 0xFF
                val b2 = bb.get(p + 2).toInt()
                ((b2 shl 16) or (b1 shl 8) or b0) / 8388608f
            }
            bits == 32 -> bb.getInt(p) / 2147483648f
            else -> 0f
        }

        private fun readTag(bb: ByteBuffer, off: Int): String =
            String(byteArrayOf(bb.get(off), bb.get(off + 1), bb.get(off + 2), bb.get(off + 3)))
    }
}
