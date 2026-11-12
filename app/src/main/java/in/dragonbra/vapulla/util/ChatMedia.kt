package `in`.dragonbra.vapulla.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A picked image, ready for Steam's chat media upload. */
class ChatMedia(val bytes: ByteArray, val fileName: String?)

/** Steam's limit for one chat upload. */
private const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024

/** Longest side of an image that has to be re-encoded; plenty for a chat. */
private const val MAX_REENCODED_SIDE = 2560

/**
 * Reads the image at [uri] for a chat upload. Steam takes PNG, JPEG and GIF up to 10 MiB, and
 * such a file is passed on untouched (which keeps a GIF animated). Anything else a phone
 * produces, HEIC or WebP say, or a file over the limit, is re-encoded as a JPEG.
 *
 * @throws IOException if the file cannot be read or decoded.
 */
suspend fun Context.readChatMedia(uri: Uri): ChatMedia = withContext(Dispatchers.IO) {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IOException("Could not open the image")

    val name = contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    if (bytes.isSteamChatImage() && bytes.size <= MAX_UPLOAD_BYTES) {
        return@withContext ChatMedia(bytes, name)
    }

    val bitmap = try {
        // ImageDecoder applies the EXIF orientation, which a plain re-encode would drop.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) {
                decoder,
                info,
                _,
            ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE

            val longest = max(info.size.width, info.size.height)
            if (longest > MAX_REENCODED_SIDE) {
                val scale = MAX_REENCODED_SIDE / longest.toFloat()
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }
        }
    } catch (e: IOException) {
        throw IOException("Could not decode the image", e)
    }

    // Step the quality down until it fits; a 2560px JPEG is far below the limit in practice.
    val jpeg = listOf(90, 75, 60).firstNotNullOfOrNull { quality ->
        ByteArrayOutputStream()
            .also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            .toByteArray()
            .takeIf { it.size <= MAX_UPLOAD_BYTES }
    } ?: throw IOException("The image is too large to upload")

    ChatMedia(jpeg, name?.substringBeforeLast('.')?.plus(".jpg"))
}

/** True for the image formats Steam's chat upload accepts as they are. */
private fun ByteArray.isSteamChatImage(): Boolean {
    fun startsWith(vararg signature: Int): Boolean = size >= signature.size &&
        signature.indices.all { (this[it].toInt() and 0xFF) == signature[it] }

    return startsWith(0x89, 0x50, 0x4E, 0x47) || // PNG
        startsWith(0xFF, 0xD8) || // JPEG
        startsWith(0x47, 0x49, 0x46, 0x38) // GIF
}
