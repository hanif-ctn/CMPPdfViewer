package com.chaintechnetwork.cmppdfviewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream

actual suspend fun downloadFromUrl(url: String): ByteArray? {
    return withContext(Dispatchers.IO) {
        try {
            val urlObj = URL(url)
            val conn = urlObj.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.connect()

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return@withContext null
            }

            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var read: Int

            conn.inputStream.use { input ->
                while (input.read(buffer).also { read = it } != -1) {
                    baos.write(buffer, 0, read)
                }
            }

            conn.disconnect()
            baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}