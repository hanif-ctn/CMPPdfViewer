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
import com.hanif.cmppicker.MediaPicker
import com.hanif.cmppicker.inappviewer.DocumentViewer
import com.hanif.cmppicker.models.SharedDocument
import com.hanif.cmppicker.rememberMediaPickerState
import com.hanif.cmppicker.utils.MediaResult
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