package com.hanif.cmppicker.inappviewer

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import com.hanif.cmppicker.models.SharedDocument
import com.hanif.cmppicker.utils.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun DocumentViewer(
    document: SharedDocument,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val data = document.data ?: return

    UIKitView(
        modifier = modifier.background(Color.White),
        factory = {
            // Pin the Kotlin ByteArray to safely pass it to NSData
            data.usePinned { pinned ->
                val nsData = data.toNSData()

                val pdfDocument = PDFDocument(data = nsData)
                val pdfView = PDFView().apply {
                    this.setDocument(pdfDocument)
                    this.setAutoScales(true)
                    this.displayMode = platform.PDFKit.kPDFDisplaySinglePageContinuous
                    this.displayDirection = platform.PDFKit.kPDFDisplayDirectionVertical
                    this.backgroundColor = platform.UIKit.UIColor.whiteColor
                }
                pdfView as UIView
            }
        }
    )
}