package com.hanif.cmppicker.utils

object MimeTypes {
    // Images
    const val IMAGE_JPEG = "image/jpeg"
    const val IMAGE_PNG = "image/png"
    const val IMAGE_GIF = "image/gif"
    const val IMAGE_BMP = "image/bmp"
    const val IMAGE_WEBP = "image/webp"
    const val IMAGE_ALL = "image/*"

    // Video
    const val VIDEO_MP4 = "video/mp4"
    const val VIDEO_MKV = "video/x-matroska"
    const val VIDEO_AVI = "video/x-msvideo"
    const val VIDEO_MOV = "video/quicktime"
    const val VIDEO_ALL = "video/*"

    // Audio
    const val AUDIO_MP3 = "audio/mpeg"
    const val AUDIO_WAV = "audio/wav"
    const val AUDIO_AAC = "audio/aac"
    const val AUDIO_ALL = "audio/*"

    // Documents
    const val PDF = "application/pdf"

    const val DOC = "application/msword"
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    const val PPT = "application/vnd.ms-powerpoint"
    const val PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation"

    const val XLS = "application/vnd.ms-excel"
    const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    const val TXT = "text/plain"
    const val RTF = "application/rtf"

    // Archives
    const val ZIP = "application/zip"
    const val RAR = "application/vnd.rar"
    const val TAR = "application/x-tar"
    const val GZ = "application/gzip"

    // Catch-all
    const val ALL_FILES = "*/*"

    // Grouped lists for convenience
    val IMAGE_TYPES = listOf(IMAGE_JPEG, IMAGE_PNG, IMAGE_GIF, IMAGE_BMP, IMAGE_WEBP)
    val VIDEO_TYPES = listOf(VIDEO_MP4, VIDEO_MKV, VIDEO_AVI, VIDEO_MOV)
    val AUDIO_TYPES = listOf(AUDIO_MP3, AUDIO_WAV, AUDIO_AAC)
    val DOCUMENT_TYPES = listOf(PDF, DOC, DOCX, PPT, PPTX, XLS, XLSX, TXT, RTF)
    val ARCHIVE_TYPES = listOf(ZIP, RAR, TAR, GZ)
}
