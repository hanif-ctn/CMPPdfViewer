package com.chaintechnetwork.cmppdfviewer

import kotlinx.coroutines.*

// Common expect declaration for downloading from URL
expect suspend fun downloadFromUrl(url: String): ByteArray?