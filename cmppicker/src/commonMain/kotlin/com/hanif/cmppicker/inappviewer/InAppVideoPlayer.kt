package com.hanif.cmppicker.inappviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hanif.cmppicker.models.SharedVideo

@Composable
expect fun InAppVideoPlayer(video: SharedVideo, modifier: Modifier = Modifier)