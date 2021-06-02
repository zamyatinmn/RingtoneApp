package com.example.ringtoneapp.player

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.Player

interface ISimplePlayer {
    fun bindContext(context: Context)
    fun init(uri: Uri): Player? = null
    fun release(){}
    fun play(){}
    fun pause(){}
    fun stop(){}
}