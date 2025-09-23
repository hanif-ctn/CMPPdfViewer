package com.hanif.cmppicker.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.hanif.cmppicker.models.SharedDocument
import com.hanif.cmppicker.models.SharedVideo

sealed class MediaResult {
    data class Image(val bitmap: ImageBitmap) : MediaResult()
    data class Video(val video: SharedVideo) : MediaResult()
    data class Document(val document: SharedDocument) : MediaResult()
}