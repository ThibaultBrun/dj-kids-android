package fr.tbrun.djkids.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Décode un fichier audio (mp3, m4a, wav, ogg…) en PCM 16 bits mono en mémoire, pour pouvoir
 * le "scratcher" à vitesse variable (lecture avant/arrière) dans [ScratchDeckEngine].
 *
 * Mono + downmix ; limité à [maxSeconds] pour borner la RAM (~1,3 Mo/s à 44,1k mono float côté moteur).
 */
object AudioDecoder {

    fun decode(context: Context, uri: Uri, maxSeconds: Int = 600): PcmSample? = runCatching {
        val extractor = MediaExtractor()
        setSource(context, extractor, uri)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i; format = f; break
            }
        }
        if (trackIndex < 0 || format == null) { extractor.release(); return null }
        extractor.selectTrack(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val srcRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val out = ArrayList<Float>(srcRate * 8)
        val maxSamples = srcRate.toLong() * maxSeconds
        val info = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inIdx = codec.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val inBuf = codec.getInputBuffer(inIdx)!!
                    val size = extractor.readSampleData(inBuf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIdx = codec.dequeueOutputBuffer(info, 10_000)
            if (outIdx >= 0) {
                val buf = codec.getOutputBuffer(outIdx)!!
                buf.order(ByteOrder.LITTLE_ENDIAN)
                val shorts = buf.asShortBuffer()
                val n = shorts.remaining()
                var i = 0
                while (i < n) {
                    var acc = 0f
                    for (c in 0 until channels) { if (i < n) { acc += shorts.get(i) / 32768f; i++ } }
                    out.add(acc / channels)
                    if (out.size >= maxSamples) break
                }
                codec.releaseOutputBuffer(outIdx, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 || out.size >= maxSamples) sawOutputEOS = true
            } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Le format de sortie peut préciser un sample rate différent ; on garde srcRate (suffisant ici).
            }
        }
        codec.stop(); codec.release(); extractor.release()

        val arr = FloatArray(out.size) { out[it] }
        PcmSample(arr, srcRate)
    }.getOrNull()

    private fun setSource(context: Context, extractor: MediaExtractor, uri: Uri) {
        if (uri.scheme == "asset") {
            val path = uri.toString().removePrefix("asset:///")
            context.assets.openFd(path).use { afd ->
                extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
        } else {
            extractor.setDataSource(context, uri, null)
        }
    }
}
