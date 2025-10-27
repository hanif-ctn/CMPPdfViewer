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
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var selectedDocument by remember { mutableStateOf<SharedDocument?>(null) }
        var documentKey by remember { mutableIntStateOf(0) }

        selectedDocument?.let { doc ->
            // Force complete recreation of DocumentViewer
            androidx.compose.runtime.key(documentKey) {
                DocumentViewer(
                    document = doc,
                    modifier = Modifier.fillMaxSize()
                ) {
                    selectedDocument = null
                }
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
                    //pickerState.pickDocument()
                }) {
                    Text("Pick Document!")
                }

                Button(onClick = {
                    documentKey++ // Increment first to force new instance
                    selectedDocument = SharedDocument(
                        name = "Adobe Sample PDF",
                        mimeType = "application/pdf",
                        data = null,
                        url = "https://www.adobe.com/support/products/enterprise/knowledgecenter/media/c4611_sample_explain.pdf"
                    )
                }) {
                    Text("Load from remote")
                }
            }
        }
    }
}