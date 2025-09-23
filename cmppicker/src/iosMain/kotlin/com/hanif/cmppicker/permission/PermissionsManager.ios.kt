package com.hanif.cmppicker.permission

import androidx.compose.runtime.Composable
import platform.AVFoundation.*
import platform.Foundation.NSURL
import platform.Photos.*
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@Composable
actual fun createPermissionsManager(callback: PermissionCallback): PermissionsManager {
    return PermissionsManager(callback)
}

actual class PermissionsManager actual constructor(private val callback: PermissionCallback) :
    PermissionHandler {

    @Composable
    actual override fun askPermission(permission: PermissionType) {
        when (permission) {
            PermissionType.CAMERA -> {
                val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                askCameraPermission(status, permission, callback)
            }
            PermissionType.GALLERY -> {
                val status = PHPhotoLibrary.authorizationStatus()
                askGalleryPermission(status, permission, callback)
            }
        }
    }

    private fun askCameraPermission(
        status: AVAuthorizationStatus,
        permission: PermissionType,
        callback: PermissionCallback
    ) {
        when (status) {
            AVAuthorizationStatusAuthorized -> {
                callback.onPermissionStatus(permission, PermissionStatus.GRANTED)
            }
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { isGranted ->
                    if (isGranted) {
                        callback.onPermissionStatus(permission, PermissionStatus.GRANTED)
                    } else {
                        callback.onPermissionStatus(permission, PermissionStatus.DENIED)
                    }
                }
            }
            AVAuthorizationStatusDenied -> {
                callback.onPermissionStatus(permission, PermissionStatus.DENIED)
            }
            else -> error("Unknown camera status $status")
        }
    }

    private fun askGalleryPermission(
        status: PHAuthorizationStatus,
        permission: PermissionType,
        callback: PermissionCallback
    ) {
        when (status) {
            PHAuthorizationStatusAuthorized -> {
                callback.onPermissionStatus(permission, PermissionStatus.GRANTED)
            }
            PHAuthorizationStatusNotDetermined -> {
                PHPhotoLibrary.requestAuthorization { newStatus ->
                    askGalleryPermission(newStatus, permission, callback)
                }
            }
            PHAuthorizationStatusDenied -> {
                callback.onPermissionStatus(permission, PermissionStatus.DENIED)
            }
            else -> error("Unknown gallery status $status")
        }
    }

    @Composable
    actual override fun isPermissionGranted(permission: PermissionType): Boolean {
        return when (permission) {
            PermissionType.CAMERA -> {
                AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) ==
                        AVAuthorizationStatusAuthorized
            }
            PermissionType.GALLERY -> {
                PHPhotoLibrary.authorizationStatus() == PHAuthorizationStatusAuthorized
            }
        }
    }

    @Composable
    actual override fun launchSettings() {
        NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
            UIApplication.sharedApplication.openURL(it)
        }
    }
}