package com.hanif.cmppicker.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hanif.cmppicker.models.SharedFile
import com.hanif.cmppicker.utils.nsDataToByteArray
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@Composable
actual fun rememberCameraManager(onResult: (SharedFile?) -> Unit): CameraManager {
    val imagePicker = UIImagePickerController()
    val app = UIApplication.sharedApplication

    val cameraDelegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage]
                        as? UIImage ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage

                if (image != null) {
                    val imageData = UIImageJPEGRepresentation(image, 0.9)
                    val bytes = imageData?.let { nsDataToByteArray(it) }
                    val shared = SharedFile(bytes, "image/jpeg", "camera_image.jpg")
                    onResult(shared)
                } else {
                    onResult(null)
                }

                picker.dismissViewControllerAnimated(true, null)
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
                onResult(null)
            }
        }
    }

    return remember {
        CameraManager(
            onLaunch = {
                imagePicker.sourceType =
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                imagePicker.allowsEditing = true
                imagePicker.delegate = cameraDelegate
                val vc = app.keyWindow?.rootViewController
                vc?.presentViewController(imagePicker, true, null)
            }
        )
    }
}

actual class CameraManager actual constructor(
    private val onLaunch: () -> Unit
) {
    actual fun launch() {
        onLaunch()
    }
}