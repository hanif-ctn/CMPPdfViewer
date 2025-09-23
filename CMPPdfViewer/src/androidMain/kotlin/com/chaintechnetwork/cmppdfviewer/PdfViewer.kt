package com.chaintechnetwork.cmppdfviewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import androidx.core.graphics.createBitmap

data class PageDimension(val width: Int, val height: Int)

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 2f
private const val ZOOM_LEVEL = 2f
private const val RENDER_SCALE_FACTOR = 2f
private const val ANIMATION_DURATION = 300

@Composable
fun PdfViewer(document: SharedDocument, contentPaddingValues: PaddingValues, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var globalScale by remember { mutableFloatStateOf(1f) }
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    val scaleAnimatable = remember { Animatable(1f) }
    val offsetXAnimatable = remember { Animatable(0f) }
    val offsetYAnimatable = remember { Animatable(0f) }

    var error by remember { mutableStateOf<String?>(null) }

    val file = remember(document) {
        val tempFile = File(context.cacheDir, "${document.name}.pdf")
        try {
            tempFile.writeBytes(document.data ?: throw IOException("Document data is null or empty"))
            tempFile
        } catch (e: Exception) {
            error = "Failed to write PDF file: ${e.message}"
            null
        }
    }

    if (file == null || error != null) {
        Text(error ?: "Invalid document", modifier = modifier.fillMaxSize())
        return
    }

    // Coroutine scope for background rendering
    val rendererScope = rememberCoroutineScope()
    // Mutex to ensure sequential page rendering (PdfRenderer is not thread-safe)
    val mutex = remember { Mutex() }

    // Produce the PdfRenderer asynchronously with error handling
    val renderer by produceState<PdfRenderer?>(null) {
        rendererScope.launch(Dispatchers.IO) {
            try {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                value = PdfRenderer(fd)
            } catch (e: IOException) {
                error = "Invalid PDF file: ${e.message}"
            } catch (e: Exception) {
                error = "Failed to load PDF: ${e.message}"
            }
        }
        // Cleanup on dispose
        awaitDispose {
            val currentRenderer = value
            rendererScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    currentRenderer?.close()
                }
            }
        }
    }

    if (error != null) {
        Text(error!!, modifier = modifier.fillMaxSize())
        return
    }

    if (renderer == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = modifier)
        }
        return
    }

    // Derive page count once renderer is ready
    val pageCount by remember(renderer) { derivedStateOf { renderer?.pageCount ?: 0 } }

    // Asynchronously load page dimensions
    val pageDimensions by produceState<List<PageDimension>>(emptyList(), renderer) {
        rendererScope.launch(Dispatchers.IO) {
            val dims = mutableListOf<PageDimension>()
            for (index in 0 until pageCount) {
                mutex.withLock {
                    renderer?.openPage(index)?.use { page ->
                        dims.add(PageDimension(page.width, page.height))
                    }
                }
            }
            value = dims
        }
    }

    if (pageDimensions.size < pageCount) {
        CircularProgressIndicator(modifier = modifier.fillMaxSize())
        return
    }

    // Update display values from animatables
    LaunchedEffect(scaleAnimatable.value, offsetXAnimatable.value, offsetYAnimatable.value) {
        globalScale = scaleAnimatable.value
        globalOffset = Offset(offsetXAnimatable.value, offsetYAnimatable.value)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                    // Global zoom and pan that affects all pages
                    val oldScale = globalScale
                    val newScale = (globalScale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)

                    // Calculate new offset
                    val newOffset = if (zoom != 1f) {
                        // Zoom gesture - center on zoom point
                        (globalOffset * (newScale / oldScale)) + (centroid * (1 - (newScale / oldScale))) + pan
                    } else {
                        // Pure pan gesture
                        globalOffset + pan
                    }

                    // Calculate bounds to prevent showing black areas
                    val containerWidth = size.width
                    val containerHeight = size.height
                    val scaledContentWidth = containerWidth * newScale
                    val scaledContentHeight = containerHeight * newScale

                    // Calculate maximum allowed offsets
                    val maxOffsetX = if (newScale > 1f) {
                        -(scaledContentWidth - containerWidth) / 2f
                    } else 0f

                    val minOffsetX = if (newScale > 1f) {
                        (scaledContentWidth - containerWidth) / 2f
                    } else 0f

                    val maxOffsetY = if (newScale > 1f) {
                        -(scaledContentHeight - containerHeight) / 2f
                    } else 0f

                    val minOffsetY = if (newScale > 1f) {
                        (scaledContentHeight - containerHeight) / 2f
                    } else 0f

                    globalScale = newScale
                    globalOffset = if (newScale <= MIN_SCALE) {
                        Offset.Zero
                    } else {
                        Offset(
                            newOffset.x.coerceIn(maxOffsetX, minOffsetX),
                            newOffset.y.coerceIn(maxOffsetY, minOffsetY)
                        )
                    }

                    // Update animatable values to current state
                    coroutineScope.launch {
                        scaleAnimatable.snapTo(globalScale)
                        offsetXAnimatable.snapTo(globalOffset.x)
                        offsetYAnimatable.snapTo(globalOffset.y)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            if (globalScale <= MIN_SCALE + 0.1f) {
                                // Zoom in with animation
                                val newScale = ZOOM_LEVEL.coerceAtMost(MAX_SCALE)
                                val newOffset = tapOffset * (1 - newScale)

                                // Calculate bounds for double-tap zoom
                                val containerWidth = size.width
                                val containerHeight = size.height
                                val scaledContentWidth = containerWidth * newScale
                                val scaledContentHeight = containerHeight * newScale

                                val maxOffsetX = if (newScale > 1f) {
                                    -(scaledContentWidth - containerWidth) / 2f
                                } else 0f

                                val minOffsetX = if (newScale > 1f) {
                                    (scaledContentWidth - containerWidth) / 2f
                                } else 0f

                                val maxOffsetY = if (newScale > 1f) {
                                    -(scaledContentHeight - containerHeight) / 2f
                                } else 0f

                                val minOffsetY = if (newScale > 1f) {
                                    (scaledContentHeight - containerHeight) / 2f
                                } else 0f

                                val clampedOffset = Offset(
                                    newOffset.x.coerceIn(maxOffsetX, minOffsetX),
                                    newOffset.y.coerceIn(maxOffsetY, minOffsetY)
                                )

                                // Animate all transforms
                                launch {
                                    scaleAnimatable.animateTo(
                                        newScale,
                                        animationSpec = tween(ANIMATION_DURATION)
                                    )
                                }
                                launch {
                                    offsetXAnimatable.animateTo(
                                        clampedOffset.x,
                                        animationSpec = tween(ANIMATION_DURATION)
                                    )
                                }
                                launch {
                                    offsetYAnimatable.animateTo(
                                        clampedOffset.y,
                                        animationSpec = tween(ANIMATION_DURATION)
                                    )
                                }
                            } else {
                                // Zoom out with animation
                                launch {
                                    scaleAnimatable.animateTo(
                                        MIN_SCALE,
                                        animationSpec = tween(ANIMATION_DURATION)
                                    )
                                }
                                launch {
                                    offsetXAnimatable.animateTo(
                                        0f,
                                        animationSpec = tween(ANIMATION_DURATION)
                                    )
                                }
                                launch {
                                    offsetYAnimatable.animateTo(
                                        0f,
                                        animationSpec = tween(ANIMATION_DURATION)
                                    )
                                }
                            }
                        }
                    }
                )
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = globalScale
                    scaleY = globalScale
                    translationX = globalOffset.x
                    translationY = globalOffset.y
                    transformOrigin = TransformOrigin.Center
                },
            contentPadding = contentPaddingValues
        ) {
            itemsIndexed(pageDimensions) { index, dim ->
                PdfPageItem(
                    pageIndex = index,
                    dimension = dim,
                    renderer = renderer,
                    mutex = mutex,
                    rendererScope = rendererScope,
                    globalScale = globalScale
                )
            }
        }
    }
}

@Composable
private fun PdfPageItem(
    pageIndex: Int,
    dimension: PageDimension,
    renderer: PdfRenderer?,
    mutex: Mutex,
    rendererScope: CoroutineScope,
    globalScale: Float
) {
    val density = LocalDensity.current

    // Calculate height based on global scale and aspect ratio
    val baseHeight = with(density) {
        (300.dp * (dimension.height.toFloat() / dimension.width.toFloat())).toPx()
    }
    val scaledHeight = with(density) { (baseHeight * globalScale).toDp() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(scaledHeight)
            .padding(vertical = 4.dp)
            .background(Color.White)
    ) {
        // Produce bitmap for this page asynchronously with scaling
        val pageBitmap by produceState<Bitmap?>(null) {
            rendererScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    try {
                        renderer?.let { r ->
                            val page = r.openPage(pageIndex)
                            // Use higher resolution for better quality when scaled
                            val renderScale = RENDER_SCALE_FACTOR * maxOf(1f, globalScale)
                            val bitmap = createBitmap(
                                (page.width * renderScale).toInt(),
                                (page.height * renderScale).toInt()
                            )
                            val transform = Matrix().apply { postScale(renderScale, renderScale) }
                            page.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            value = bitmap
                        }
                    } catch (_: Exception) {
                        // Handle per-page errors if needed
                    }
                }
            }
            awaitDispose {
                value?.recycle()
            }
        }

        if (pageBitmap == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Image(
                bitmap = pageBitmap!!.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
internal fun ImageViewer(document: SharedDocument, modifier: Modifier) {
    val bitmap = remember(document) {
        document.data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = document.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offset = if (scale > 1f) {
                                offset + pan
                            } else {
                                Offset.Zero
                            }
                        }
                    },
                contentScale = ContentScale.Fit
            )
        } ?: ErrorMessage("Invalid image format")
    }
}

@Composable
internal fun TextViewer(document: SharedDocument, modifier: Modifier) {
    val text = remember(document) { document.data?.decodeToString() ?: "" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun UnsupportedFileViewer(document: SharedDocument, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Please select a supported file - only PDF supported for now",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            document.mimeType ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* Handle with external app */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open with external app")
        }
    }
}

@Composable
internal fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}