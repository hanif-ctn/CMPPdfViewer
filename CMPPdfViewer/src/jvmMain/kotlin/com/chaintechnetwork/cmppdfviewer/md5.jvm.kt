package com.chaintechnetwork.cmppdfviewer
actual fun String.md5(): String {
    val digest = java.security.MessageDigest.getInstance("MD5")
    val messageDigest = digest.digest(this.toByteArray())
    return messageDigest.joinToString("") { "%02x".format(it) }
}