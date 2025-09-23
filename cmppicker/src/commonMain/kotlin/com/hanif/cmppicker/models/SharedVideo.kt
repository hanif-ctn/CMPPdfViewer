package com.hanif.cmppicker.models

data class SharedVideo(
    val name: String,
    val mimeType: String,
    val data: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SharedVideo
        if (name != other.name) return false
        if (mimeType != other.mimeType) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}