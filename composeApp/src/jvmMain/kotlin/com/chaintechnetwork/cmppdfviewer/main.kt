package com.chaintechnetwork.cmppdfviewer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.chaintechnetwork.cmppdfviewer.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KotlinProject",
    ) {
        App()
    }
}