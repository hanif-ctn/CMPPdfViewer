package com.hanif.cmppicker.externalviewer

import com.hanif.cmppicker.models.SharedDocument

expect suspend fun openDocumentInExternalViewer(document: SharedDocument): Boolean
