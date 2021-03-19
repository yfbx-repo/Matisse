package com.zhihu.matisse.internal.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi

/**
 * http://stackoverflow.com/a/27271131/4739220
 */
/**
 * Get a file path from a Uri. This will get the the path for Storage Access
 * Framework Documents, as well as the _data field for the MediaStore and
 * other file-based ContentProviders.
 *
 * @param context The context.
 * @param uri     The Uri to query.
 * @author paulburke
 */
fun Uri.filePath(context: Context): String? {
    return when {
        DocumentsContract.isDocumentUri(context, this) -> {
            when {
                isExternalStorageDocument() -> {
                    val docId = DocumentsContract.getDocumentId(this)
                    val split = docId.split(":")
                    val type = split[0]

                    if ("primary".equals(type, true)) {
                        Environment.getExternalStorageDirectory().path + "/" + split[1]
                    } else {
                        // TODO handle non-primary volumes
                        null
                    }
                }
                isDownloadsDocument() -> {
                    val id = DocumentsContract.getDocumentId(this)
                    val downloadUri = Uri.parse("content://downloads/public_downloads")
                    val contentUri = ContentUris.withAppendedId(downloadUri, id.toLong())
                    contentUri.getDataColumn(context, null, null)
                }
                isMediaDocument() -> {
                    val docId = DocumentsContract.getDocumentId(this)
                    val split = docId.split(":")
                    val type = split[0]
                    val args = split[1]
                    val contentUri = when (type) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> return null
                    }
                    contentUri.getDataColumn(context, "_id=?", arrayOf(args))
                }
                else -> null
            }
        }
        "content".equals(scheme, true) -> getDataColumn(context, null, null)
        "file".equals(scheme, true) -> path
        else -> null
    }
}

/**
 * Get the value of the data column for this Uri. This is useful for
 * MediaStore Uris, and other file-based ContentProviders.
 *
 * @param context       The context.
 * @param selection     (Optional) Filter used in the query.
 * @param selectionArgs (Optional) Selection arguments used in the query.
 * @return The value of the _data column, which is typically a file path.
 */
private fun Uri.getDataColumn(context: Context, selection: String?, selectionArgs: Array<String>?): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(column)

    try {
        cursor = context.contentResolver.query(this, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(columnIndex)
        }
    } finally {
        cursor?.close()
    }
    return null
}


/**
 * @return Whether the Uri authority is ExternalStorageProvider.
 */
private fun Uri.isExternalStorageDocument(): Boolean {
    return "com.android.externalstorage.documents" == authority
}

/**
 * @return Whether the Uri authority is DownloadsProvider.
 */
private fun Uri.isDownloadsDocument(): Boolean {
    return "com.android.providers.downloads.documents" == authority
}

/**
 * @return Whether the Uri authority is MediaProvider.
 */
private fun Uri.isMediaDocument(): Boolean {
    return "com.android.providers.media.documents" == authority
}