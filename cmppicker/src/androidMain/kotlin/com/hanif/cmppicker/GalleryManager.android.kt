package com.hanif.cmppicker

import android.content.ContentResolver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.hanif.cmppicker.models.SharedFile
import com.hanif.cmppicker.utils.uriToSharedImage

@Composable
actual fun rememberGalleryManager(
    isSingleSelection: Boolean,
    type: PickerType,
    mimeTypes: List<String>,
    onResult: (List<SharedFile>?) -> Unit
): GalleryManager {
    val context = LocalContext.current
    val contentResolver: ContentResolver = context.contentResolver

    val singlePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) {
                onResult(null)
                return@rememberLauncherForActivityResult
            }
            val item = uriToSharedImage(contentResolver, uri)
            onResult(listOfNotNull(item))
        }

    val multiPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            val items = uris.mapNotNull { uri -> uriToSharedImage(contentResolver, uri) }
            onResult(items)
        }

    val singleDocumentPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                onResult(null)
                return@rememberLauncherForActivityResult
            }
            val item = uriToSharedImage(contentResolver, uri)
            onResult(listOfNotNull(item))
        }

    val multiDocumentPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val items = uris.mapNotNull { uri -> uriToSharedImage(contentResolver, uri) }
            onResult(items)
        }

    val mediaType = when (type) {
        PickerType.IMAGE -> ActivityResultContracts.PickVisualMedia.ImageOnly
        PickerType.VIDEO -> ActivityResultContracts.PickVisualMedia.VideoOnly
        PickerType.IMAGE_AND_VIDEO -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
        else -> null
    }

    return remember(isSingleSelection, type, mimeTypes) {
        GalleryManager(onLaunch = {
            when (type) {
                PickerType.DOCUMENT -> {
                    val filters = if (mimeTypes.isEmpty()) arrayOf("*/*") else mimeTypes.toTypedArray()
                    if (isSingleSelection) {
                        singleDocumentPickerLauncher.launch(filters)
                    } else {
                        multiDocumentPickerLauncher.launch(filters)
                    }
                }
                PickerType.IMAGE, PickerType.VIDEO, PickerType.IMAGE_AND_VIDEO -> {
                    if (isSingleSelection) {
                        singlePickerLauncher.launch(PickVisualMediaRequest(mediaType = mediaType!!))
                    } else {
                        multiPickerLauncher.launch(PickVisualMediaRequest(mediaType = mediaType!!))
                    }
                }
                PickerType.CAMERA -> {
                    // handled by rememberCameraManager
                }
            }
        })
    }
}

actual class GalleryManager actual constructor(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch()
    }
}
