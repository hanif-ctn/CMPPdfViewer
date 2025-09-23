// iosMain/kotlin/com/hanif/cmppicker/GalleryManager.ios.kt
package com.hanif.cmppicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hanif.cmppicker.models.SharedFile
import com.hanif.cmppicker.utils.nsDataToByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify

@Composable
actual fun rememberGalleryManager(
    isSingleSelection: Boolean,
    type: PickerType,
    mimeTypes: List<String>,
    onResult: (List<SharedFile>?) -> Unit
): GalleryManager {
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    val app = UIApplication.sharedApplication

    // PHPickerViewController delegate for images/videos
    val phPickerDelegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(
                picker: PHPickerViewController,
                didFinishPicking: List<*>
            ) {
                picker.dismissViewControllerAnimated(flag = true, completion = null)

                @Suppress("UNCHECKED_CAST")
                val results = didFinishPicking as List<PHPickerResult>

                if (results.isEmpty()) {
                    onResult(null)
                    return
                }

                val dispatchGroup = dispatch_group_create()
                val sharedFiles = mutableListOf<SharedFile>()

                for (result in results) {
                    dispatch_group_enter(dispatchGroup)

                    if (result.itemProvider.hasItemConformingToTypeIdentifier("public.image")) {
                        result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { nsData, _ ->
                            scope.launch(Dispatchers.Main) {
                                nsData?.let { data ->
                                    val bytes = nsDataToByteArray(data)
                                    sharedFiles.add(SharedFile(bytes, "image/jpeg", null))
                                }
                                dispatch_group_leave(dispatchGroup)
                            }
                        }
                    } else if (result.itemProvider.hasItemConformingToTypeIdentifier("public.movie")) {
                        result.itemProvider.loadFileRepresentationForTypeIdentifier("public.movie") { url, _ ->
                            scope.launch(Dispatchers.Main) {
                                url?.let { fileUrl ->
                                    val data = NSData.dataWithContentsOfURL(fileUrl)
                                    data?.let {
                                        val bytes = nsDataToByteArray(it)
                                        val filename = fileUrl.lastPathComponent
                                        sharedFiles.add(SharedFile(bytes, "video/mp4", filename))
                                    }
                                }
                                dispatch_group_leave(dispatchGroup)
                            }
                        }
                    } else {
                        dispatch_group_leave(dispatchGroup)
                    }
                }

                dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
                    scope.launch(Dispatchers.Main) {
                        onResult(if (sharedFiles.isNotEmpty()) sharedFiles else null)
                    }
                }
            }
        }
    }

    // UIImagePickerController delegate (single video/image fallback)
    val imagePicker = UIImagePickerController()
    val galleryDelegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol,
            UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage
                    ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage

                if (image != null) {
                    val imageData = UIImageJPEGRepresentation(image, 0.9) ?: run {
                        picker.dismissViewControllerAnimated(true, null)
                        onResult(null)
                        return
                    }
                    val bytes = nsDataToByteArray(imageData)
                    onResult(listOf(SharedFile(bytes, "image/jpeg", null)))
                    picker.dismissViewControllerAnimated(true, null)
                    return
                }

                val mediaURL = didFinishPickingMediaWithInfo[UIImagePickerControllerMediaURL] as? NSURL
                if (mediaURL != null) {
                    val data = NSData.dataWithContentsOfURL(mediaURL) ?: run {
                        picker.dismissViewControllerAnimated(true, null)
                        onResult(null)
                        return
                    }
                    val bytes = nsDataToByteArray(data)
                    val filename = mediaURL.lastPathComponent
                    onResult(listOf(SharedFile(bytes, "video/mp4", filename)))
                    picker.dismissViewControllerAnimated(true, null)
                    return
                }

                picker.dismissViewControllerAnimated(true, null)
                onResult(null)
            }
        }
    }

    // Document picker delegate
    val docDelegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                val resultList = mutableListOf<SharedFile>()
                for (urlAny in didPickDocumentsAtURLs) {
                    val url = urlAny as? NSURL ?: continue
                    val data = NSData.dataWithContentsOfURL(url) ?: continue
                    val bytes = nsDataToByteArray(data)
                    val name = url.lastPathComponent
                    resultList += SharedFile(bytes, null, name)
                }
                onResult(if (resultList.isNotEmpty()) resultList else null)
            }
        }
    }

    return remember(isSingleSelection, type, mimeTypes) {
        GalleryManager {
            when (type) {
                PickerType.IMAGE -> {
                    val config = PHPickerConfiguration().apply {
                        selectionLimit = if (isSingleSelection) 1 else 0
                        filter = PHPickerFilter.imagesFilter
                        selection = PHPickerConfigurationSelectionOrdered
                    }
                    val phPicker = PHPickerViewController(configuration = config)
                    phPicker.delegate = phPickerDelegate
                    app.keyWindow?.rootViewController?.presentViewController(phPicker, true, null)
                }

                PickerType.VIDEO -> {
                    val config = PHPickerConfiguration().apply {
                        selectionLimit = if (isSingleSelection) 1 else 0 // 👈 1 = single, 0 = unlimited
                        filter = PHPickerFilter.videosFilter
                        selection = PHPickerConfigurationSelectionOrdered
                    }
                    val phPicker = PHPickerViewController(configuration = config)
                    phPicker.delegate = phPickerDelegate
                    app.keyWindow?.rootViewController?.presentViewController(phPicker, true, null)
                }

                PickerType.IMAGE_AND_VIDEO -> {
                    val config = PHPickerConfiguration().apply {
                        selectionLimit = if (isSingleSelection) 1 else 0
                        filter = PHPickerFilter.anyFilterMatchingSubfilters(
                            listOf(PHPickerFilter.imagesFilter, PHPickerFilter.videosFilter)
                        )
                        selection = PHPickerConfigurationSelectionOrdered
                    }
                    val phPicker = PHPickerViewController(configuration = config)
                    phPicker.delegate = phPickerDelegate
                    app.keyWindow?.rootViewController?.presentViewController(phPicker, true, null)
                }

                PickerType.DOCUMENT -> {
                    // Map MIME -> UTType
                    val utTypes = mimeTypes.mapNotNull { mime ->
                        when (mime) {
                            "application/pdf" -> UTTypePDF
                            "text/plain" -> UTTypePlainText
                            "application/rtf" -> UTTypeRTF
                            "application/zip" -> UTTypeZIP
                            // For common office/compound types we fall back to the generic "item" type
                            // because UTType(filenameExtension=...) isn't available from Kotlin/Native.
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-powerpoint",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/vnd.rar" -> UTTypeItem
                            else -> null
                        }
                    }

                    val allowedTypes = if (utTypes.isNotEmpty()) utTypes else listOf(UTTypeItem)

                    val docPicker = UIDocumentPickerViewController(
                        forOpeningContentTypes = allowedTypes,
                        asCopy = true
                    )
                    docPicker.delegate = docDelegate
                    docPicker.allowsMultipleSelection = !isSingleSelection
                    docPicker.modalPresentationStyle = UIModalPresentationFormSheet
                    app.keyWindow?.rootViewController?.presentViewController(docPicker, true, null)
                }

                PickerType.CAMERA -> {
                    // TODO: camera picker (optional)
                }
            }
        }
    }
}

actual class GalleryManager actual constructor(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch()
    }
}