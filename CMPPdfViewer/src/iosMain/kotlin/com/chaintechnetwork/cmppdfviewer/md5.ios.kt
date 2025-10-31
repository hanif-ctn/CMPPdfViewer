package com.chaintechnetwork.cmppdfviewer

import kotlinx.cinterop.*
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_LONG
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding

@OptIn(ExperimentalForeignApi::class)
actual fun String.md5(): String {
    val nsString = this as NSString
    val nsData = nsString.dataUsingEncoding(NSUTF8StringEncoding)
        ?: throw IllegalArgumentException("Failed to encode string to UTF-8")
    val bytes = nsData.bytes
    val length = nsData.length
    val md5 = UByteArray(16)
    md5.usePinned { output ->
        CC_MD5(
            bytes,
            length.toUInt(),
            output.addressOf(0)
        )
    }
    return md5.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}