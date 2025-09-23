package com.hanif.cmppicker.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData {
    return this.usePinned {
        NSData.dataWithBytes(
            it.addressOf(0),
            this.size.toULong()
        ).copy() as NSData // ensure memory safety
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun nsDataToByteArray(data: NSData): ByteArray {
    val length = data.length.toInt()
    if (length == 0) return ByteArray(0)

    return ByteArray(length).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, length.toULong())
        }
    }
}