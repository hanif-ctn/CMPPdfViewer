package com.hanif.cmppicker.externalviewer

import android.content.Intent
import androidx.core.content.FileProvider
import com.hanif.cmppicker.AndroidContext
import com.hanif.cmppicker.models.SharedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual suspend fun openVideoInExternalPlayer(video: SharedVideo): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val context = AndroidContext.applicationContext
            val videoData = video.data ?: return@withContext false

            val tempDir = File(context.cacheDir, "videos")
            if (!tempDir.exists()) tempDir.mkdirs()

            val extension = when (video.mimeType) {
                "video/mp4" -> ".mp4"
                "video/avi" -> ".avi"
                "video/mov", "video/quicktime" -> ".mov"
                "video/3gpp" -> ".3gp"
                "video/webm" -> ".webm"
                else -> ".mp4"
            }

            val safeName = video.name.replace("/", "_")
            val tempFile = File(tempDir, "$safeName$extension")

            FileOutputStream(tempFile).use { fos ->
                fos.write(videoData)
                fos.flush()
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "org.hanif.imagepickerdemo.provider",
                tempFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val packageManager = context.packageManager
            val resolveInfo = intent.resolveActivity(packageManager)

            if (resolveInfo != null) {
                withContext(Dispatchers.Main) { context.startActivity(intent) }
                true
            } else {
                val chooserIntent = Intent.createChooser(intent, "Open video with...")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                withContext(Dispatchers.Main) {
                    context.startActivity(chooserIntent)
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}