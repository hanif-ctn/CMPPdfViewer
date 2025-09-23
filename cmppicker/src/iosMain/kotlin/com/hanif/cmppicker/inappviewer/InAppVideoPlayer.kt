package com.hanif.cmppicker.inappviewer

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.hanif.cmppicker.models.SharedVideo
import com.hanif.cmppicker.utils.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.AVKit.*
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun InAppVideoPlayer(
    video: SharedVideo,
    modifier: Modifier
) {
    // Keep AVPlayerViewController alive across recompositions
    val controller = remember { AVPlayerViewController() }

    UIKitView(
        modifier = modifier,
        factory = {
            // Write ByteArray to temp file
            val tempDir = NSTemporaryDirectory()
            val extension = when (video.mimeType) {
                "video/mp4" -> ".mp4"
                "video/avi" -> ".avi"
                "video/mov", "video/quicktime" -> ".mov"
                "video/3gpp" -> ".3gp"
                "video/webm" -> ".webm"
                else -> ".mp4"
            }
            val filePath = tempDir + "/" + video.name.replace("/", "_") + extension
            val nsData = video.data?.toNSData()
            nsData?.writeToFile(filePath, true)

            // Load into AVPlayer
            val url = NSURL.fileURLWithPath(filePath)
            val player = AVPlayer(uRL = url)
            controller.player = player
            controller.showsPlaybackControls = true

            // Autoplay
            player.play()

            controller.view
        },
        update = { _ ->
            // Called on recomposition if needed
        }
    )
}