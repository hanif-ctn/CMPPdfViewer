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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.zIndex
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView
import platform.PDFKit.kPDFDisplayDirectionVertical
import platform.PDFKit.kPDFDisplaySinglePageContinuous
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class, ExperimentalMaterial3Api::class)
@Composable
actual fun DocumentViewer(
    document: SharedDocument,
    modifier: Modifier,
    onBack: () -> Unit
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    var topAppBarHeight by remember { mutableStateOf(0.dp) }
    var loadedDocument by remember { mutableStateOf(document) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var retryTrigger by remember { mutableStateOf(0) }
    val localDensity = LocalDensity.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(document.url, retryTrigger) {
        if (loadedDocument.data == null && document.url != null) {
            isLoading = true
            loadError = null
            progress = 0f
            withContext(Dispatchers.Default) {
                try {
                    val fileManager = NSFileManager.defaultManager
                    val cachesURLs = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
                    val cachesURL = cachesURLs.firstOrNull() as? NSURL
                        ?: throw Exception("Unable to get caches directory")
                    val filename = document.url!!.md5() + ".pdf"
                    val cacheURL = cachesURL.URLByAppendingPathComponent(filename)
                        ?: throw Exception("Unable to create cache URL")
                    val cachePath = cacheURL.path ?: throw Exception("Unable to get cache path")

                    if (fileManager.fileExistsAtPath(cachePath)) {
                        val nsData = NSData.dataWithContentsOfFile(cachePath)
                            ?: throw Exception("Unable to read cached file")
                        val data = nsData.toByteArray()
                        scope.launch(Dispatchers.Main) {
                            progress = 1f
                            loadedDocument = document.copy(
                                data = data,
                                size = data.size.toLong(),
                                mimeType = "application/pdf"
                            )
                            isLoading = false
                        }
                        return@withContext
                    }

                    val (data, mimeType) = downloadUrlWithProgress(document.url!!) { p ->
                        scope.launch(Dispatchers.Main) {
                            progress = p
                        }
                    }
                    val nsData = data.toNSData()
                    nsData.writeToURL(cacheURL, true)

                    scope.launch(Dispatchers.Main) {
                        progress = 1f
                        loadedDocument = document.copy(
                            data = data,
                            size = data.size.toLong(),
                            mimeType = mimeType ?: "application/pdf"
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
                            progress = { progress },
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
                val data = loadedDocument.data ?: return@Box
                UIKitView(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(top = if (isControlsVisible) topAppBarHeight else 0.dp),
                    factory = {
                        data.usePinned { pinned ->
                            val nsData = NSData.dataWithBytes(
                                pinned.addressOf(0),
                                data.size.toULong()
                            )

                            val pdfDocument = PDFDocument(data = nsData)
                            val pdfView = PDFView().apply {
                                this.setDocument(pdfDocument)
                                this.setAutoScales(true)
                                this.displayMode = kPDFDisplaySinglePageContinuous
                                this.displayDirection = kPDFDisplayDirectionVertical
                                this.backgroundColor = UIColor.whiteColor
                            }
                            pdfView as UIView
                        }
                    }
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
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(1f) // Ensure TopAppBar is above PDFView
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

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData = this.usePinned {
    NSData.dataWithBytes(it.addressOf(0), this.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val byteArray = ByteArray(size)
    byteArray.usePinned {
        this.getBytes(it.addressOf(0).reinterpret(), this.length)
    }
    return byteArray
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun downloadUrlWithProgress(
    url: String,
    onProgress: (Float) -> Unit
): Pair<ByteArray, String?> = suspendCancellableCoroutine { cont ->
    val nsUrl = NSURL.URLWithString(url) ?: run {
        cont.resumeWithException(IllegalArgumentException("Invalid URL"))
        return@suspendCancellableCoroutine
    }
    val request = NSMutableURLRequest.requestWithURL(nsUrl)
    request.HTTPMethod = "GET"

    var totalLength: Long = -1
    var mimeType: String? = null
    val receivedData = NSMutableData()

    val delegate = object : NSObject(), NSURLSessionDataDelegateProtocol, NSURLSessionDelegateProtocol {
        override fun URLSession(
            session: NSURLSession,
            dataTask: NSURLSessionDataTask,
            didReceiveResponse: NSURLResponse,
            completionHandler: (NSURLSessionResponseDisposition) -> Unit
        ) {
            val httpResponse = didReceiveResponse as? NSHTTPURLResponse
            if (httpResponse?.statusCode != 200L) {
                completionHandler(NSURLSessionResponseCancel)
                cont.resumeWithException(Exception("HTTP error: ${httpResponse?.statusCode}"))
                return
            }
            totalLength = httpResponse?.expectedContentLength ?: -1
            mimeType = httpResponse?.MIMEType
            completionHandler(NSURLSessionResponseAllow)
        }

        override fun URLSession(
            session: NSURLSession,
            dataTask: NSURLSessionDataTask,
            didReceiveData: NSData
        ) {
            receivedData.appendData(didReceiveData)
            if (totalLength > 0) {
                val p = receivedData.length.toFloat() / totalLength.toFloat()
                onProgress(p)
            }
        }

        override fun URLSession(
            session: NSURLSession,
            task: NSURLSessionTask,
            didCompleteWithError: NSError?
        ) {
            if (didCompleteWithError != null) {
                cont.resumeWithException(Exception(didCompleteWithError.localizedDescription))
            } else {
                cont.resume(Pair(receivedData.toByteArray(), mimeType))
            }
            session.finishTasksAndInvalidate()
        }
    }

    val configuration = NSURLSessionConfiguration.defaultSessionConfiguration
    val session = NSURLSession.sessionWithConfiguration(configuration, delegate, NSOperationQueue.mainQueue)
    val task = session.dataTaskWithRequest(request)
    task.resume()

    cont.invokeOnCancellation {
        task.cancel()
        session.invalidateAndCancel()
    }
}