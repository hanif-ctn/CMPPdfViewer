package com.chaintechnetwork.cmppdfviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun DocumentViewer(document: SharedDocument, modifier: Modifier, onBack: () -> Unit) {
    var isControlsVisible by remember { mutableStateOf(true) }
    var topAppBarHeight by remember { mutableStateOf(0.dp) }
    var loadedDocument by remember { mutableStateOf(document) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var retryTrigger by remember { mutableStateOf(0) }
    val localDensity = LocalDensity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(document.url, retryTrigger) {
        if (loadedDocument.data == null && document.url != null) {
            isLoading = true
            loadError = null
            progress = 0f
            withContext(Dispatchers.IO) {
                try {
                    val cacheDir = context.cacheDir
                    val filename = document.url.md5() + ".pdf"
                    val cacheFile = File(cacheDir, filename)
                    if (cacheFile.exists()) {
                        scope.launch(Dispatchers.Main) {
                            progress = 1f
                            loadedDocument = document.copy(
                                data = cacheFile.readBytes(),
                                size = cacheFile.length(),
                                mimeType = "application/pdf" // Set for cached files
                            )
                            isLoading = false
                        }
                        return@withContext
                    }

                    val urlObj = URL(document.url)
                    val conn = urlObj.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.connect()
                    val mimeType = conn.contentType ?: "application/octet-stream"
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
                        scope.launch(Dispatchers.Main) {
                            progress = p
                        }
                    }
                    input.close()
                    conn.disconnect()
                    val data = baos.toByteArray()
                    cacheFile.writeBytes(data)
                    scope.launch(Dispatchers.Main) {
                        progress = 1f
                        loadedDocument = document.copy(
                            data = data,
                            size = data.size.toLong(),
                            mimeType = mimeType
                        )
                        isLoading = false
                    }
                } catch (e: Exception) {
                    scope.launch(Dispatchers.Main) {
                        loadError = "Download failed: ${e.message}"
                        isLoading = false
                    }
                }
            }
        }
    }

    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            kotlinx.coroutines.delay(3000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures {
                    isControlsVisible = !isControlsVisible
                }
            }
    ) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Downloading... ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            loadError != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = loadError!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { retryTrigger++ }) {
                            Text("Retry")
                        }
                    }
                }
            }
            loadedDocument.mimeType?.lowercase()?.contains("pdf") == true -> {
                PdfViewer(
                    document = loadedDocument,
                    contentPaddingValues = if (isControlsVisible) PaddingValues(top = topAppBarHeight) else PaddingValues(),
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This is not supported. MimeType: ${loadedDocument.mimeType}")
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
                    .onGloballyPositioned { layoutCoordinates ->
                        with(localDensity) {
                            topAppBarHeight = layoutCoordinates.size.height.toDp()
                        }
                    },
                title = {
                    Text(
                        text = document.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle share */ }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                }
            )
        }
    }
}