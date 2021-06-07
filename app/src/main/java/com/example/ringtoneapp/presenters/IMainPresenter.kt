package com.example.ringtoneapp.presenters

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.Player

interface IMainPresenter {
    var isFirstStarted: Boolean
    fun bindContext(context: Context)
    fun init(uri: Uri): Player? = null
    fun release(){}
    fun play(){}
    fun pause(){}
    fun stop(){}
    fun trim(start: Int, end: Int, fileName: String, fadeIn: Boolean, fadeOut: Boolean):Int = -1
    fun getAudioName():String = ""
    fun getTime(seconds: Int):String = ""
    fun getAbsolutePath(): String = ""
}