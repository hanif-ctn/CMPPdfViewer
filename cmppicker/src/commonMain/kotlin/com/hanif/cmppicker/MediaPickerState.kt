package com.hanif.cmppicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.hanif.cmppicker.camera.rememberCameraManager
import com.hanif.cmppicker.models.SharedDocument
import com.hanif.cmppicker.models.SharedVideo
import com.hanif.cmppicker.permission.PermissionCallback
import com.hanif.cmppicker.permission.PermissionStatus
import com.hanif.cmppicker.permission.PermissionType
import com.hanif.cmppicker.permission.createPermissionsManager
import com.hanif.cmppicker.utils.MediaResult
import kotlinx.coroutines.launch

class MediaPickerState internal constructor() {
    internal var pendingAction: PickerType? by mutableStateOf(null)
    internal var documentMimeTypes: List<String> by mutableStateOf(emptyList())
    internal var isSingleSelection: Boolean by mutableStateOf(false)
    internal var isSingleSelectionForImage: Boolean by mutableStateOf(false)

    fun pickImage(isSingle: Boolean = false) {
        isSingleSelectionForImage = isSingle
        pendingAction = PickerType.IMAGE
    }

    fun pickVideo(isSingle: Boolean = false) {
        isSingleSelection = isSingle
        pendingAction = PickerType.VIDEO
    }

    fun pickMixed(isSingle: Boolean = false) {
        isSingleSelection = isSingle
        pendingAction = PickerType.IMAGE_AND_VIDEO
    }

    fun pickDocument(
        mimeTypes: List<String> = emptyList(),
        isSingle: Boolean = false
    ) {
        documentMimeTypes = mimeTypes
        isSingleSelection = isSingle
        pendingAction = PickerType.DOCUMENT
    }

    fun pickCamera() {
        isSingleSelection = true // Camera always single
        pendingAction = PickerType.CAMERA
    }
}

@Composable
fun rememberMediaPickerState(): MediaPickerState {
    return remember { MediaPickerState() }
}


@Composable
fun MediaPicker(
    state: MediaPickerState,
    onResult: (MediaResult) -> Unit,
    onPermissionDenied: (PermissionType) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val permissionsManager = createPermissionsManager(object : PermissionCallback {
        override fun onPermissionStatus(permissionType: PermissionType, status: PermissionStatus) {
            if (status == PermissionStatus.DENIED) {
                onPermissionDenied(permissionType)
            }
        }
    })

    // Camera manager
    val cameraManager = rememberCameraManager { file ->
        coroutineScope.launch {
            file?.toImageBitmap()?.let { onResult(MediaResult.Image(it)) }
        }
    }

    // Gallery / Document managers
    val imageGalleryManager = rememberGalleryManager(
        isSingleSelection = state.isSingleSelectionForImage,
        type = PickerType.IMAGE
    ) { images ->
        coroutineScope.launch {
            val bitmaps = images?.mapNotNull { it.toImageBitmap() } ?: emptyList()
            bitmaps.forEach { onResult(MediaResult.Image(it)) }
        }
    }

    val videoGalleryManager = rememberGalleryManager(
        isSingleSelection = state.isSingleSelection,
        type = PickerType.VIDEO
    ) { videos ->
        coroutineScope.launch {
            val videoObjs = videos?.map {
                SharedVideo(it.name ?: "Unknown", it.mimeType!!, it.toByteArray())
            } ?: emptyList()
            videoObjs.forEach { onResult(MediaResult.Video(it)) }
        }
    }

    val mixedGalleryManager = rememberGalleryManager(
        isSingleSelection = state.isSingleSelection,
        type = PickerType.IMAGE_AND_VIDEO
    ) { media ->
        coroutineScope.launch {
            media?.forEach { m ->
                when {
                    m.mimeType?.startsWith("image/") == true ->
                        m.toImageBitmap()?.let { onResult(MediaResult.Image(it)) }
                    m.mimeType?.startsWith("video/") == true ->
                        onResult(MediaResult.Video(
                            SharedVideo(m.name ?: "Unknown", m.mimeType!!, m.toByteArray())
                        ))
                }
            }
        }
    }

    val documentPickerManager = rememberGalleryManager(
        isSingleSelection = state.isSingleSelection,
        type = PickerType.DOCUMENT,
        mimeTypes = state.documentMimeTypes
    ) { docs ->
        coroutineScope.launch {
            val files = docs?.map {
                SharedDocument(it.name ?: "Unknown", it.mimeType, it.toByteArray())
            } ?: emptyList()
            files.forEach { onResult(MediaResult.Document(it)) }
        }
    }

    // Handle launch actions
    when (state.pendingAction) {
        PickerType.CAMERA -> {
            if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
                cameraManager.launch()
            } else {
                permissionsManager.askPermission(PermissionType.CAMERA)
            }
        }
        PickerType.IMAGE -> {
            if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
                imageGalleryManager.launch()
            } else {
                permissionsManager.askPermission(PermissionType.GALLERY)
            }
        }
        PickerType.VIDEO -> {
            if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
                videoGalleryManager.launch()
            } else {
                permissionsManager.askPermission(PermissionType.GALLERY)
            }
        }
        PickerType.IMAGE_AND_VIDEO -> {
            if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
                mixedGalleryManager.launch()
            } else {
                permissionsManager.askPermission(PermissionType.GALLERY)
            }
        }
        PickerType.DOCUMENT -> {
            if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
                documentPickerManager.launch()
            } else {
                permissionsManager.askPermission(PermissionType.GALLERY)
            }
        }
        null -> {}
    }

    // Reset after handling
    LaunchedEffect(state.pendingAction) {
        if (state.pendingAction != null) {
            state.pendingAction = null
        }
    }
}


