package com.example.ringtoneapp.trimmer

interface ITrimmer {
    fun trim(
        start: Int, end: Int, fileName: String, fadeIn: Boolean,
        fadeOut: Boolean, originalPath: String
    ): Int = -1

    val absolutePath: String
}