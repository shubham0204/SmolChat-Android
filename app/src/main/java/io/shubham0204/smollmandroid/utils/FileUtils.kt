package io.shubham0204.smollmandroid.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

class FileUtils {
    companion object {
        /**
         * Copies a file from a given [Uri] to the application's internal storage directory.
         *
         * @param context The context used to access the content resolver and internal files directory.
         * @param fileUri The [Uri] of the source file to be copied.
         * @param fileName The name to be assigned to the copied file within [Context.getFilesDir].
         */
        fun copyFile(context: Context, fileUri: Uri, fileName: String) {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                FileOutputStream(File(context.filesDir, fileName)).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }


        /**
         * Retrieves the display name (file name) from a given [Uri].
         *
         * This method queries the [android.content.ContentResolver] to find the
         * [android.provider.OpenableColumns.DISPLAY_NAME] associated with the provided URI.
         *
         * @param context The context used to access the ContentResolver.
         * @param uri The [Uri] of the file.
         * @return The file name as a [String], or null if the column is not present or the query fails.
         */
        fun getFileNameFromUri(context: Context, uri: Uri): String? {
            var fileName: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            return fileName
        }

        fun getFilePathFromUri(context: Context, uri: Uri): String {
            val contentResolver = context.contentResolver
            val fd = contentResolver.openFileDescriptor(uri, "r")
            val detachedFd = fd?.detachFd()
            return "/proc/self/fd/$detachedFd"
        }
    }
}
