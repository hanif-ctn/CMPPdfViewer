package com.hanif.cmppicker.inappviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hanif.cmppicker.models.SharedDocument

@Composable
expect fun DocumentViewer(
    document: SharedDocument,
    modifier: Modifier = Modifier,
    onBack : () -> Unit
)