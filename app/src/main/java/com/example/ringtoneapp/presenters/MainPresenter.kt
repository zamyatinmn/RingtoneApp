package com.example.ringtoneapp.presenters

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.example.ringtoneapp.player.ISimplePlayer
import com.example.ringtoneapp.trimmer.ITrimmer
import com.google.android.exoplayer2.Player
import javax.inject.Inject

class MainPresenter @Inject constructor(private val player: ISimplePlayer,
                                        private val trimmer: ITrimmer): IMainPresenter {
    private lateinit var context: Context
    private lateinit var uri: Uri
    override var isFirstStarted = false


    override fun bindContext(context: Context){
        this.context = context
        player.bindContext(context)
    }

    override fun init(uri: Uri): Player {
        this.uri = uri
        return player.init(uri)!!
    }

    override fun release() {
        player.release()
        isFirstStarted = false
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun stop() {
        player.stop()
    }

    override fun trim(start: Int, end: Int, fileName: String, fadeIn: Boolean,
             fadeOut: Boolean):Int{
        return trimmer.trim(start, end, fileName, fadeIn, fadeOut, getRealPathFromUri(context, uri))
    }

    private fun getRealPathFromUri(applicationContext: Context, containUri: Uri?): String {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Audio.Media.DATA)
            cursor = applicationContext.contentResolver.query(
                containUri!!, proj,
                null, null, null
            )
            val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            cursor?.close()
        }
    }

    override fun getAudioName(): String {
        val path = getRealPathFromUri(context, uri)
        return path.substring(path.lastIndexOf("/") + 1)
    }

    override fun getTime(seconds: Int): String {
        val rem = seconds % 3600
        val mn = rem / 60
        val sec = rem % 60
        return String.format("%02d:%02d", mn, sec)
    }

    override fun getAbsolutePath(): String{
        return trimmer.absolutePath
    }
}