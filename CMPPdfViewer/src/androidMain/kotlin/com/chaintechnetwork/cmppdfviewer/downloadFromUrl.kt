package com.chaintechnetwork.cmppdfviewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

actual suspend fun downloadFromUrl(url: String): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            connectTimeout = 10000
            readTimeout = 10000
            requestMethod = "GET"
        }
        connection.inputStream.use { it.readBytes() }
    }.getOrNull()
}