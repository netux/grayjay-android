package com.futo.platformplayer.api.media.models

enum class ThumbnailType(val value : String) {
    MAIN("main"),
    ALTERNATIVE("alternative");

    companion object {
        fun fromValue(value: String): ThumbnailType? {
            return entries.find { it.value == value }
        }
    }
}