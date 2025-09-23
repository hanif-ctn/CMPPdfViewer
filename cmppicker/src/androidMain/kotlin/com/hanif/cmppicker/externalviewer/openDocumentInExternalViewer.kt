// androidMain
package com.hanif.cmppicker.externalviewer

import android.content.Intent
import androidx.core.content.FileProvider
import com.hanif.cmppicker.AndroidContext
import com.hanif.cmppicker.models.SharedDocument
import com.hanif.cmppicker.utils.getFileExtensionFromMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual suspend fun openDocumentInExternalViewer(document: SharedDocument): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val context = AndroidContext.applicationContext
            val documentData = document.data ?: run { return@withContext false }
            val tempDir = File(context.cacheDir, "documents")
            if (!tempDir.exists()) tempDir.mkdirs()

            val extension = getFileExtensionFromMimeType(document.mimeType)
                ?: document.name.substringAfterLast(".", "tmp")

            val sanitizedName = document.name.replace("/", "_").replace("\\", "_")
            val tempFile = File(tempDir, "$sanitizedName.$extension")

            FileOutputStream(tempFile).use { fos ->
                fos.write(documentData)
                fos.flush()
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "org.hanif.imagepickerdemo.provider",
                tempFile
            )

            val mimeType = document.mimeType ?: "application/octet-stream"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val packageManager = context.packageManager
            val resolveInfo = intent.resolveActivity(packageManager)

            if (resolveInfo != null) {
                withContext(Dispatchers.Main) { context.startActivity(intent) }
                true
            } else {
                val chooserIntent = Intent.createChooser(intent, "Open document with...")
                chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                withContext(Dispatchers.Main) {
                    context.startActivity(chooserIntent)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
