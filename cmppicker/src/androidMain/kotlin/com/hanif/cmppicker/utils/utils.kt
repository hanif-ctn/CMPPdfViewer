package com.hanif.cmppicker.utils

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.hanif.cmppicker.models.SharedFile

internal fun uriToSharedImage(contentResolver: ContentResolver, uri: Uri): SharedFile? {
    return try {
        contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            val mime = contentResolver.getType(uri)
            val name = queryDisplayName(contentResolver, uri)
            SharedFile(bytes, mime, name)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = cursor.getString(idx)
        }
    } finally {
        cursor?.close()
    }
    return name
}

internal fun getFileExtensionFromMimeType(mimeType: String?): String? {
    return when (mimeType) {
        "application/pdf" -> "pdf"
        "application/msword" -> "doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        "application/vnd.ms-excel" -> "xls"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
        "application/vnd.ms-powerpoint" -> "ppt"
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
        "text/plain" -> "txt"
        "text/html" -> "html"
        "text/css" -> "css"
        "text/javascript" -> "js"
        "application/json" -> "json"
        "application/xml", "text/xml" -> "xml"
        "application/zip" -> "zip"
        "application/x-rar-compressed" -> "rar"
        "application/x-7z-compressed" -> "7z"
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "video/mp4" -> "mp4"
        "video/avi" -> "avi"
        "video/mov", "video/quicktime" -> "mov"
        "audio/mp3", "audio/mpeg" -> "mp3"
        "audio/wav" -> "wav"
        else -> null
    }
}
