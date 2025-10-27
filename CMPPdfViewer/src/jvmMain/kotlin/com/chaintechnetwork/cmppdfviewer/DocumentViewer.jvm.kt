package com.chaintechnetwork.cmppdfviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.skia.Image as SkiaImage
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun DocumentViewer(
    document: SharedDocument,
    modifier: Modifier,
    onBack: () -> Unit
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    var topAppBarHeight by remember { mutableStateOf(0.dp) }
    var zoomLevel by remember { mutableStateOf(1f) }

    // FIXED: Use remember(document) to reset when document changes
    var loadedDocument by remember(document) { mutableStateOf(document) }
    var isLoading by remember(document) { mutableStateOf(false) }
    var loadError by remember(document) { mutableStateOf<String?>(null) }
    var progress by remember(document) { mutableStateOf(0f) }
    var retryTrigger by remember { mutableStateOf(0) }

    val localDensity = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Infer MIME type if missing
    LaunchedEffect(document) {
        if (loadedDocument.mimeType == null || loadedDocument.mimeType?.isBlank() ?: false) {
            val ext = loadedDocument.name.substringAfterLast('.', "").lowercase()
            loadedDocument = loadedDocument.copy(
                mimeType = when (ext) {
                    "pdf" -> "application/pdf"
                    "png", "jpg", "jpeg", "bmp", "gif" -> "image/$ext"
                    "txt" -> "text/plain"
                    "md" -> "text/markdown"
                    "json" -> "application/json"
                    "xml" -> "application/xml"
                    "csv" -> "text/csv"
                    "log" -> "text/plain"
                    else -> "application/octet-stream"
                }
            )
        }
    }

    // Download document if needed
    LaunchedEffect(document, retryTrigger) {
        if (document.data == null && document.url != null) {
            isLoading = true
            loadError = null
            progress = 0f
            withContext(Dispatchers.IO) {
                try {
                    val cacheDir = File(System.getProperty("user.home"), ".cmp-pdfviewer-cache")
                    cacheDir.mkdirs()
                    val filename = document.url!!.md5() + ".pdf"
                    val cacheFile = File(cacheDir, filename)

                    // Check cache first
                    if (cacheFile.exists()) {
                        val cachedData = cacheFile.readBytes()
                        withContext(Dispatchers.Main) {
                            progress = 1f
                            loadedDocument = document.copy(
                                data = cachedData,
                                size = cacheFile.length(),
                                mimeType = loadedDocument.mimeType ?: "application/pdf"
                            )
                            isLoading = false
                        }
                        return@withContext
                    }

                    // Download from URL
                    val urlObj = URL(document.url)
                    val conn = urlObj.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.connect()
                    val mimeType = conn.contentType ?: "application/pdf"
                    val total = conn.contentLengthLong.coerceAtLeast(1L)
                    val input = conn.inputStream
                    val baos = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        baos.write(buffer, 0, read)
                        downloaded += read
                        val p = (downloaded.toFloat() / total).coerceAtMost(1f)
                        scope.launch { progress = p }
                    }
                    input.close()
                    conn.disconnect()
                    val data = baos.toByteArray()
                    cacheFile.writeBytes(data)
                    withContext(Dispatchers.Main) {
                        progress = 1f
                        loadedDocument = document.copy(
                            data = data,
                            size = data.size.toLong(),
                            mimeType = mimeType
                        )
                        isLoading = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        loadError = "Download failed: ${e.message}"
                        isLoading = false
                    }
                }
            }
        } else if (document.data != null) {
            // If document already has data, use it directly
            loadedDocument = document
            isLoading = false
        }
    }

    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) kotlinx.coroutines.delay(3000).also { isControlsVisible = false }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures { isControlsVisible = !isControlsVisible }
            }
    ) {
        when {
            isLoading -> DownloadingView(progress)
            loadError != null -> ErrorWithRetry(loadError!!) { retryTrigger++ }
            loadedDocument.data == null -> ErrorViewer("No document data available")
            else -> {
                val mime = loadedDocument.mimeType ?: ""
                when {
                    mime.contains("pdf", ignoreCase = true) -> PdfViewer(loadedDocument, zoomLevel)
                    mime.startsWith("image/", ignoreCase = true) -> ImageViewer(loadedDocument, zoomLevel)
                    mime.startsWith("text/") -> TextViewer(loadedDocument, zoomLevel)
                    else -> UnsupportedViewer(loadedDocument)
                }
            }
        }

        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { topAppBarHeight = with(localDensity) { it.size.height.toDp() } },
                title = { Text(text = document.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.5f) }) { Icon(Icons.Default.ZoomOut, "Zoom Out") }
                    Text("${(zoomLevel * 100).toInt()}%", modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { zoomLevel = (zoomLevel + 0.25f).coerceAtMost(4f) }) { Icon(Icons.Default.ZoomIn, "Zoom In") }
                    IconButton(onClick = { /* Share */ }) { Icon(Icons.Default.Share, "Share") }
                }
            )
        }
    }
}

// ----------------------- VIEWERS -----------------------

@Composable
private fun DownloadingView(progress: Float) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(0.8f))
            Spacer(Modifier.height(16.dp))
            Text("Downloading... ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorWithRetry(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun ErrorViewer(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            //Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(64.dp), taint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ImageViewer(document: SharedDocument, zoom: Float) {
    val bitmap = remember(document.data) {
        document.data?.let { ImageIO.read(ByteArrayInputStream(it))?.toComposeImageBitmap() }
    }
    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = document.name, modifier = Modifier.graphicsLayer(scaleX = zoom, scaleY = zoom).padding(16.dp), contentScale = ContentScale.Fit)
        } ?: ErrorViewer("Failed to load image")
    }
}

@Composable
private fun TextViewer(document: SharedDocument, zoom: Float) {
    val text = remember(document.data) { document.data?.toString(Charsets.UTF_8) ?: "No data" }
    SelectionContainer {
        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(text, fontSize = (14 * zoom).sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PdfViewer(document: SharedDocument, zoom: Float) {
    var pages by remember(document.data) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(document.data) {
        withContext(Dispatchers.IO) {
            try {
                document.data?.let { data ->
                    val pdf = Loader.loadPDF(data)
                    val renderer = PDFRenderer(pdf)
                    val imgs = mutableListOf<ImageBitmap>()
                    for (i in 0 until pdf.numberOfPages) imgs.add(renderer.renderImageWithDPI(i, 150f).toComposeImageBitmap())
                    pages = imgs
                    pdf.close()
                } ?: run { error = "No PDF data" }
            } catch (e: Exception) {
                error = "Failed to load PDF: ${e.message}"
            } finally { loading = false }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            error != null -> ErrorViewer(error!!)
            else -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                pages.forEachIndexed { idx, bitmap ->
                    Card(modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = zoom, scaleY = zoom).padding(vertical = 8.dp)) {
                        Column {
                            Image(bitmap = bitmap, contentDescription = "Page ${idx + 1}", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnsupportedViewer(document: SharedDocument) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("Preview not available", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(document.name, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ----------------------- Utilities -----------------------
private fun BufferedImage.toComposeImageBitmap(): ImageBitmap {
    val baos = ByteArrayOutputStream()
    ImageIO.write(this, "PNG", baos)
    return SkiaImage.makeFromEncoded(baos.toByteArray()).toComposeImageBitmap()
}