package com.example.ringtoneapp.player

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer

class SimplePlayer : ISimplePlayer {
    private lateinit var simpleExoplayer: SimpleExoPlayer
    private lateinit var context: Context

    override fun bindContext(context: Context) {
        this.context = context
    }

    override fun init(uri: Uri): Player {
        simpleExoplayer = SimpleExoPlayer.Builder(context).build()
        val mediaItem = MediaItem.fromUri(uri)
        simpleExoplayer.setMediaItem(mediaItem)
        simpleExoplayer.prepare()
        return simpleExoplayer
    }

    override fun stop() {
        simpleExoplayer.stop()
    }

    override fun release() {
        simpleExoplayer.release()
    }

    override fun play() {
        simpleExoplayer.play()
    }

    override fun pause() {
        simpleExoplayer.pause()
    }
}