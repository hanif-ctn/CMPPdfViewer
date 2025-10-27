package com.chaintechnetwork.cmppdfviewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewer(
    document: SharedDocument,
    contentPaddingValues: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    // Assume data is loaded
    val data = document.data ?: return
    var pages by remember { mutableStateOf<List<androidx.compose.ui.graphics.ImageBitmap>>(emptyList()) }
    var isRendering by remember { mutableStateOf(true) }
    var renderError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(data) {
        withContext(Dispatchers.IO) {
            try {
                // Temp file for PdfRenderer
                val tempFile = File.createTempFile("pdf", ".pdf")
                FileOutputStream(tempFile).use { it.write(data) }
                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)

                val pageImages = mutableListOf<androidx.compose.ui.graphics.ImageBitmap>()
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(
                        (page.width * 1.5f).toInt(), // Scale for better quality
                        (page.height * 1.5f).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )
                    pageImages.add(bitmap.asImageBitmap())
                    page.close()
                }

                renderer.close()
                pfd.close()
                tempFile.delete()

                withContext(Dispatchers.Main) {
                    pages = pageImages
                    isRendering = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    renderError = "Failed to render PDF: ${e.message}"
                    isRendering = false
                }
            }
        }
    }

    Column(
        modifier = modifier.padding(contentPaddingValues)
    ) {
        when {
            isRendering -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            renderError != null -> {
                Text(renderError!!, modifier = Modifier.padding(16.dp))
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    pages.forEachIndexed { index, bitmap ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Page ${index + 1} of ${pages.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(8.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}