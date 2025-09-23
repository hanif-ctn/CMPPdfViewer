package com.hanif.cmppicker

import androidx.compose.runtime.Composable
import com.hanif.cmppicker.models.SharedFile

@Composable
expect fun rememberGalleryManager(
    isSingleSelection: Boolean = false,
    type: PickerType,
    mimeTypes: List<String> = listOf(),
    onResult: (List<SharedFile>?) -> Unit
): GalleryManager


expect class GalleryManager(
    onLaunch: () -> Unit
) {
    fun launch()
}

enum class PickerType {
    IMAGE,
    VIDEO,
    IMAGE_AND_VIDEO,
    DOCUMENT,
    CAMERA
}