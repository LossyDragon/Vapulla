package `in`.dragonbra.vapulla.util.decoders

import android.graphics.BitmapFactory
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.ByteString.Companion.toByteString
import timber.log.Timber

/**
 * Coil decoder for `.ico` files, which Steam uses for client icons. Android's
 * [BitmapFactory] can read the embedded PNG or BMP frame directly.
 */
class IconDecoder(private val source: SourceFetchResult) : Decoder {

    override suspend fun decode(): DecodeResult? = try {
        val bytes = source.source.source().readByteArray()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bitmap ->
            DecodeResult(
                image = bitmap.asImage(),
                isSampled = false,
            )
        }
    } catch (e: Exception) {
        Timber.e(e, "Something happened while decoding an ico file.")
        null
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            val validMimeType = result.mimeType?.contains("ico", ignoreCase = true) == true
            val validHeader = result.source
                .source()
                .peek()
                .rangeEquals(0, ICO_HEADER.toByteString())

            return if (validMimeType || validHeader) IconDecoder(result) else null
        }

        private companion object {
            // https://en.wikipedia.org/wiki/ICO_(file_format)#Header
            val ICO_HEADER = byteArrayOf(0, 0, 1, 0)
        }
    }
}
