package org.chaintechnetwork.cmppdfviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import multiplatform.network.cmpfilepicker.MediaPicker
import multiplatform.network.cmpfilepicker.inappviewer.DocumentViewer
import multiplatform.network.cmpfilepicker.models.SharedDocument
import multiplatform.network.cmpfilepicker.rememberMediaPickerState
import multiplatform.network.cmpfilepicker.utils.MediaResult
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {

        val pickerState = rememberMediaPickerState()
        var selectedDocument by remember { mutableStateOf<SharedDocument?>(null) }

        // Media Picker Component
        MediaPicker(
            state = pickerState,
            onResult = { result ->
                when (result) {
                    is MediaResult.Document -> {
                        selectedDocument = result.document
                    }

                    else -> {

                    }
                }
            },
            onPermissionDenied = { deniedPermission ->

            }
        )

        selectedDocument?.let {
            DocumentViewer(document = it, modifier = Modifier.fillMaxSize()) {

            }
        }

        if (selectedDocument == null) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    pickerState.pickDocument()
                }) {
                    Text("Pick Document!")
                }
            }
        }
    }
}