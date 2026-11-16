package `in`.dragonbra.vapulla.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import java.io.File

/**
 * Folders outside the app's own, as a place to install downloads to. Android only lets an
 * app write plain files there with access to all files, which the user grants on a system
 * settings screen; that exists from Android 11 on.
 */
object StorageFolders {

    /** Whether this Android version has the all-files access that any folder needs. */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** Whether the user has given the app access to all files. */
    fun hasAccess(): Boolean = isSupported && Environment.isExternalStorageManager()

    /** Opens the system screen where access to all files is given to this app. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun accessSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData(Uri.fromParts("package", context.packageName, null))

    /**
     * The file path of a folder picked with the system's folder picker, which only gives an
     * address of its own kind. Null for a folder that is not on a storage volume, such as
     * one in a cloud drive.
     */
    fun pathOf(tree: Uri): String? {
        // "primary:Games/Steam" for the device's own storage, "1A2B-3C4D:Games" for a card.
        val id = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull()
            ?: return null
        val volume = id.substringBefore(':', missingDelimiterValue = "")
        val folder = id.substringAfter(':', missingDelimiterValue = "")

        val root = when {
            tree.authority != EXTERNAL_STORAGE_AUTHORITY || volume.isEmpty() -> return null
            volume == PRIMARY_VOLUME -> Environment.getExternalStorageDirectory()
            else -> File("/storage", volume)
        }

        return File(root, folder).absolutePath
    }

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val PRIMARY_VOLUME = "primary"
}
