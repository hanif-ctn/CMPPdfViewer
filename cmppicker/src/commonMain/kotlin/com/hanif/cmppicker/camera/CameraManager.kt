package com.hanif.cmppicker.camera

import androidx.compose.runtime.Composable
import com.hanif.cmppicker.models.SharedFile

@Composable
expect fun rememberCameraManager(onResult: (SharedFile?) -> Unit): CameraManager

expect class CameraManager(
    onLaunch: () -> Unit
) {
    fun launch()
}