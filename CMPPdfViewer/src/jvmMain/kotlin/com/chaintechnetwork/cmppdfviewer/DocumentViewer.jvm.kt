package com.chaintechnetwork.cmppdfviewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.skia.Image as SkiaImage
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun DocumentViewer(
    document: SharedDocument,
    modifier: Modifier,
    onBack: () -> Unit
) {
    var zoomLevel by remember { mutableStateOf(1f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = document.name,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.5f) }) {
                        Icon(Icons.Default.ZoomOut, "Zoom Out")
                    }
                    Text(
                        text = "${(zoomLevel * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { zoomLevel = (zoomLevel + 0.25f).coerceAtMost(4f) }) {
                        Icon(Icons.Default.ZoomIn, "Zoom In")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (document.data == null) {
                ErrorViewer("No document data available")
            } else {
                when {
                    document.mimeType?.startsWith("image/") == true -> {
                        ImageViewer(document, zoomLevel)
                    }

                    document.mimeType == "application/pdf" -> {
                        PdfViewer(document, zoomLevel)
                    }

                    document.mimeType?.startsWith("text/") == true ||
                            document.name.endsWith(".txt", ignoreCase = true) ||
                            document.name.endsWith(".md", ignoreCase = true) ||
                            document.name.endsWith(".json", ignoreCase = true) ||
                            document.name.endsWith(".xml", ignoreCase = true) ||
                            document.name.endsWith(".csv", ignoreCase = true) ||
                            document.name.endsWith(".log", ignoreCase = true) -> {
                        TextViewer(document, zoomLevel)
                    }

                    else -> {
                        UnsupportedViewer(document)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageViewer(document: SharedDocument, zoomLevel: Float) {
    val imageBitmap = remember(document) {
        try {
            document.data?.let { data ->
                val bufferedImage = ImageIO.read(ByteArrayInputStream(data))
                bufferedImage?.toComposeImageBitmap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = document.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = zoomLevel,
                        scaleY = zoomLevel
                    )
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            ErrorViewer("Failed to load image")
        }
    }
}

@Composable
private fun PdfViewer(document: SharedDocument, zoomLevel: Float) {
    var pages by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(document) {
        withContext(Dispatchers.IO) {
            try {
                document.data?.let { data ->
                    val pdfDocument = Loader.loadPDF(data)
                    val renderer = PDFRenderer(pdfDocument)

                    val pageImages = mutableListOf<ImageBitmap>()
                    for (i in 0 until pdfDocument.numberOfPages) {
                        val image = renderer.renderImageWithDPI(i, 150f)
                        pageImages.add(image.toComposeImageBitmap())
                    }

                    pages = pageImages
                    pdfDocument.close()
                    isLoading = false
                } ?: run {
                    error = "No PDF data available"
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error = "Failed to load PDF: ${e.message}"
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            error != null -> {
                ErrorViewer(error!!)
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
                                .graphicsLayer(
                                    scaleX = zoomLevel,
                                    scaleY = zoomLevel
                                )
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

@Composable
private fun TextViewer(document: SharedDocument, zoomLevel: Float) {
    val textContent = remember(document) {
        try {
            document.data?.let { data ->
                String(data, Charsets.UTF_8)
            } ?: "No data available"
        } catch (e: Exception) {
            e.printStackTrace()
            "Failed to read file: ${e.message}"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // File info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${document.size} bytes",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${textContent.lines().size} lines",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Text content
            SelectionContainer {
                Text(
                    text = textContent,
                    fontSize = (14 * zoomLevel).sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = (20 * zoomLevel).sp
                )
            }
        }
    }
}

@Composable
private fun UnsupportedViewer(document: SharedDocument) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Preview not available",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "File type: ${document.mimeType ?: "Unknown"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = document.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Size: ${formatFileSize(document.size)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorViewer(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

// Extension function to convert BufferedImage to ImageBitmap
private fun BufferedImage.toComposeImageBitmap(): ImageBitmap {
    val baos = ByteArrayOutputStream()
    ImageIO.write(this, "PNG", baos)
    val bytes = baos.toByteArray()
    return SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
}

// Helper function to format file size
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}