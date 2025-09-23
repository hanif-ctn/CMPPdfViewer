package com.chaintechnetwork.cmppdfviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun DocumentViewer(
    document: SharedDocument,
    modifier: Modifier = Modifier,
    onBack : () -> Unit
)