package com.hanif.cmppicker.models

import androidx.compose.ui.graphics.ImageBitmap

expect class SharedFile {
    fun toByteArray(): ByteArray?
    fun toImageBitmap(): ImageBitmap?
    val mimeType: String?
    val name: String?
}