package com.sharn.handpan.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

sealed interface AudioDecodeResult {
    data class Success(val audio: PcmAudio) : AudioDecodeResult
    data class Failure(val code: AudioDecodeFailure, val diagnostic: String? = null) : AudioDecodeResult
}

enum class AudioDecodeFailure {
    INVALID_URI,
    NO_AUDIO_TRACK,
    UNSUPPORTED_FORMAT,
    TOO_LONG,
    INVALID_METADATA,
    DECODE_ERROR,
    CANCELLED
}

/** Android-only adapter. The transcription core never depends on Uri, MediaExtractor or MediaCodec. */
class AndroidAudioDecoder(
    private val maxDurationMs: Long = OfflineHandpanTranscriber.MAX_DURATION_MS,
    private val maxSampleRateHz: Int = 192_000,
    private val maxChannels: Int = 8
) {
    suspend fun decode(context: Context, uri: Uri): AudioDecodeResult {
        if (uri.toString().isBlank()) return AudioDecodeResult.Failure(AudioDecodeFailure.INVALID_URI)
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            coroutineContext.ensureActive()
            extractor.setDataSource(context, uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return AudioDecodeResult.Failure(AudioDecodeFailure.NO_AUDIO_TRACK)
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: return AudioDecodeResult.Failure(AudioDecodeFailure.UNSUPPORTED_FORMAT)
            val sampleRate = format.getIntegerOrNull(MediaFormat.KEY_SAMPLE_RATE)
                ?: return AudioDecodeResult.Failure(AudioDecodeFailure.INVALID_METADATA)
            val channels = format.getIntegerOrNull(MediaFormat.KEY_CHANNEL_COUNT)
                ?: return AudioDecodeResult.Failure(AudioDecodeFailure.INVALID_METADATA)
            val durationUs = format.getLongOrNull(MediaFormat.KEY_DURATION)
            if (sampleRate !in 8_000..maxSampleRateHz || channels !in 1..maxChannels) {
                return AudioDecodeResult.Failure(AudioDecodeFailure.INVALID_METADATA)
            }
            if (durationUs != null && durationUs > maxDurationMs * 1_000L) {
                return AudioDecodeResult.Failure(AudioDecodeFailure.TOO_LONG)
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            val output = FloatAccumulator(maxFrames = ((maxDurationMs * sampleRate) / 1_000L).toInt())
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val input = codec.getInputBuffer(inputIndex) ?: throw IllegalStateException("Missing input buffer")
                        val size = extractor.readSampleData(input, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val timestamp = extractor.sampleTime.coerceAtLeast(0L)
                            if (timestamp > maxDurationMs * 1_000L) {
                                codec.queueInputBuffer(inputIndex, 0, 0, timestamp, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, size, timestamp, 0)
                                extractor.advance()
                            }
                        }
                    }
                }
                val outputIndex = codec.dequeueOutputBuffer(info, 10_000L)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    outputIndex >= 0 -> {
                        val buffer = codec.getOutputBuffer(outputIndex)
                        if (buffer != null && info.size > 0) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            val pcmEncoding = codec.outputFormat.getIntegerOrNull(MediaFormat.KEY_PCM_ENCODING)
                            if (pcmEncoding != null && pcmEncoding != android.media.AudioFormat.ENCODING_PCM_16BIT) {
                                return AudioDecodeResult.Failure(AudioDecodeFailure.UNSUPPORTED_FORMAT)
                            }
                            while (buffer.remaining() >= 2 * channels) {
                                var sum = 0.0
                                repeat(channels) { sum += buffer.short.toInt() / 32768.0 }
                                output.add((sum / channels).toFloat().coerceIn(-1f, 1f))
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    }
                }
            }
            AudioDecodeResult.Success(PcmAudio(output.toArray(), sampleRate, channels))
        } catch (_: kotlinx.coroutines.CancellationException) {
            AudioDecodeResult.Failure(AudioDecodeFailure.CANCELLED)
        } catch (error: Exception) {
            AudioDecodeResult.Failure(AudioDecodeFailure.DECODE_ERROR, error::class.simpleName)
        } finally {
            try { codec?.stop() } catch (_: Exception) { }
            codec?.release()
            extractor.release()
        }
    }

    private class FloatAccumulator(private val maxFrames: Int) {
        private var values = FloatArray(minOf(maxFrames, 16_384).coerceAtLeast(1))
        private var size = 0
        fun add(value: Float) {
            if (size >= maxFrames) return
            if (size == values.size) values = values.copyOf(minOf(maxFrames, values.size * 2))
            values[size++] = value
        }
        fun toArray(): FloatArray = values.copyOf(size)
    }
}

private fun MediaFormat.getIntegerOrNull(key: String): Int? =
    if (containsKey(key)) getInteger(key) else null

private fun MediaFormat.getLongOrNull(key: String): Long? =
    if (containsKey(key)) getLong(key) else null