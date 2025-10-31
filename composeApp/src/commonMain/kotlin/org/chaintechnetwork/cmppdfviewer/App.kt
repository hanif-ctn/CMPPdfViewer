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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chaintechnetwork.cmppdfviewer.DocumentViewer
import com.chaintechnetwork.cmppdfviewer.SharedDocument
import multiplatform.network.cmpfilepicker.MediaPicker
import multiplatform.network.cmpfilepicker.rememberMediaPickerState
import multiplatform.network.cmpfilepicker.utils.MediaResult
import multiplatform.network.cmpfilepicker.utils.MimeTypes
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var selectedDocument by remember { mutableStateOf<SharedDocument?>(null) }
        var documentKey by remember { mutableIntStateOf(0) }
        val pickerState = rememberMediaPickerState()

        // Media Picker Component
        MediaPicker(
            state = pickerState,
            onResult = { result ->
                when (result) {
                    is MediaResult.Document -> {
                        result.document.apply {
                            selectedDocument = SharedDocument(
                                name = name,
                                mimeType = mimeType,
                                data = data,
                                size = size
                            )
                        }
                    }

                    else -> {

                    }
                }
            },
            onPermissionDenied = { deniedPermission ->

            }
        )

        selectedDocument?.let { doc ->
            DocumentViewer(
                document = doc,
                modifier = Modifier.fillMaxSize()
            ) {
                selectedDocument = null
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
                    pickerState.pickDocument(mimeTypes = listOf(MimeTypes.PDF))
                }) {
                    Text("Pick Document!")
                }

                Button(onClick = {
                    selectedDocument = SharedDocument(
                        name = "Adobe Sample PDF",
                        mimeType = "application/pdf",
                        url = "https://www.adobe.com/support/products/enterprise/knowledgecenter/media/c4611_sample_explain.pdf"
                    )
                }) {
                    Text("Load from remote")
                }
            }
        }
    }
}