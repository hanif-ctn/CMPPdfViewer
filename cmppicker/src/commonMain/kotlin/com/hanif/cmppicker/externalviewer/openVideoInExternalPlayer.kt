package com.hanif.cmppicker.externalviewer

import com.hanif.cmppicker.models.SharedDocument
import com.hanif.cmppicker.models.SharedVideo

expect suspend fun openVideoInExternalPlayer(video: SharedVideo): Boolean