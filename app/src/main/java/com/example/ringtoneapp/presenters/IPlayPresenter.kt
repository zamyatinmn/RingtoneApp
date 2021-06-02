package com.example.ringtoneapp.presenters

import android.content.Context
import com.google.android.exoplayer2.Player

interface IPlayPresenter {
    fun bindContext(context: Context)
    fun init(): Player? = null
    fun release(){}
    fun play(){}
    fun pause(){}
    fun setRingtone() {}
}