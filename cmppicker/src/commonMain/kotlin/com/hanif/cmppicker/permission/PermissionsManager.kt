package com.hanif.cmppicker.permission

import androidx.compose.runtime.Composable

expect class PermissionsManager(callback: PermissionCallback) : PermissionHandler {
    @Composable
    override fun askPermission(permission: PermissionType)

    @Composable
    override fun isPermissionGranted(permission: PermissionType): Boolean

    @Composable
    override fun launchSettings()
}

interface PermissionCallback {
    fun onPermissionStatus(permissionType: PermissionType, status: PermissionStatus)
}

@Composable
expect fun createPermissionsManager(callback: PermissionCallback): PermissionsManager

enum class PermissionStatus {
    GRANTED, DENIED, SHOW_RATIONAL
}

enum class PermissionType {
    CAMERA, GALLERY
}