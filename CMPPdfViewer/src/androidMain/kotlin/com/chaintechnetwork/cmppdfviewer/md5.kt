// jvmMain and androidMain - Actual for md5
package com.chaintechnetwork.cmppdfviewer

import java.security.MessageDigest

actual fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(this.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}