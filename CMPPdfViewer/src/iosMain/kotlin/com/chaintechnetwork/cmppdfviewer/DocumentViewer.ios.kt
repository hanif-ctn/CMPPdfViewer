package com.chaintechnetwork.cmppdfviewer

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView
import platform.PDFKit.kPDFDisplayDirectionVertical
import platform.PDFKit.kPDFDisplaySinglePageContinuous
import platform.UIKit.UIColor
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
                    this.displayMode = kPDFDisplaySinglePageContinuous
                    this.displayDirection = kPDFDisplayDirectionVertical
                    this.backgroundColor = UIColor.whiteColor
                }
                pdfView as UIView
            }
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData {
    return this.usePinned {
        NSData.dataWithBytes(
            it.addressOf(0),
            this.size.toULong()
        ).copy() as NSData // ensure memory safety
    }
}
