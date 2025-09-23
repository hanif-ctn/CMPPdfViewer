package com.hanif.cmppicker.models

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual class SharedFile(
    private val bytes: ByteArray?,
    actual val mimeType: String?,
    actual val name: String?
) {
    actual fun toByteArray(): ByteArray? = bytes

    actual fun toImageBitmap(): ImageBitmap? {
        if (bytes == null) return null
        // only decode for image MIME types
        if (mimeType?.startsWith("image/") != true) return null
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return bmp.asImageBitmap()
    }
}