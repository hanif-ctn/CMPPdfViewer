// shared/androidMain/DocumentViewer.android.kt
package com.hanif.cmppicker.inappviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanif.cmppicker.models.SharedDocument
import com.hanif.cmppicker.models.SharedVideo
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun DocumentViewer(document: SharedDocument, modifier: Modifier, onBack: () -> Unit) {
    var isControlsVisible by remember { mutableStateOf(true) }
    var topAppBarHeight by remember { mutableStateOf(0.dp) }
    val localDensity = LocalDensity.current

    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(3000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    isControlsVisible = !isControlsVisible
                }
            }
    ) {
        when {
            document.mimeType?.contains("pdf") == true ->
                PdfViewer(document, contentPaddingValues = if (isControlsVisible) PaddingValues(top = topAppBarHeight) else PaddingValues(), modifier = modifier)

            document.mimeType?.startsWith("image/") == true ->
                ImageViewer(document, Modifier.fillMaxSize())

            document.mimeType?.startsWith("video/") == true ->
                InAppVideoPlayer(
                    video = SharedVideo(document.name, document.mimeType, document.data),
                    modifier = Modifier.fillMaxSize()
                )

            document.mimeType?.startsWith("text/") == true ||
                    document.mimeType in listOf("application/json", "application/xml") ->
                TextViewer(document, Modifier.fillMaxSize())

            else -> UnsupportedFileViewer(document, Modifier.fillMaxSize())
        }

        // Top bar with document info
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
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle share */ }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    }
}
