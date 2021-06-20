package com.example.ringtoneapp.presenters

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import com.example.ringtoneapp.player.ISimplePlayer
import com.example.ringtoneapp.R
import com.google.android.exoplayer2.Player
import java.io.File
import javax.inject.Inject


class PlayPresenter @Inject constructor(private val player: ISimplePlayer) : IPlayPresenter {

    private lateinit var context: Context

    override fun bindContext(context: Context) {
        this.context = context
        player.bindContext(context)
    }

    override fun init(): Player? {
        return player.init(getUriFromIntent())
    }

    override fun release() {
        player.release()
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun setRingtone() {
        RingtoneManager.setActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_RINGTONE, getUriFromIntent()
        )
        val defaultUri =
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        if (getUriFromIntent() == defaultUri) {
            Toast.makeText(
                context,
                R.string.ringtone_installed, Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                R.string.wrong, Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun setContactRingtone(){
        val values = ContentValues()
        values.put(
            ContactsContract.Contacts.CUSTOM_RINGTONE,
            getUriFromIntent().toString()
        )
        context.getContentResolver().update(contactUri, values, null, null)
    }

    private fun getUriFromIntent(): Uri {
        val intent = (context as Activity).intent
        if (intent != null) {
            val filePath = intent.getStringExtra("filepath")
            return Uri.fromFile(File(filePath!!))
        }
        return Uri.EMPTY
    }

}