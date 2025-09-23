package com.hanif.cmppicker.models

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

// actual SharedImage for iOS - ONLY DEFINE THIS ONCE IN YOUR PROJECT
actual class SharedFile(
    private val bytes: ByteArray?,
    actual val mimeType: String?,
    actual val name: String?
) {
    actual fun toByteArray(): ByteArray? = bytes

    actual fun toImageBitmap(): ImageBitmap? {
        if (bytes == null) return null
        if (mimeType?.startsWith("image/") != true) {
            // try to decode anyway (if it's actually an image but missing mime)
            return try {
                Image.Companion.makeFromEncoded(bytes).toComposeImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
        return try {
            Image.Companion.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }
}