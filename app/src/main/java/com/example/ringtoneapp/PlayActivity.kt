package com.example.ringtoneapp

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ringtoneapp.databinding.ActivityPlayBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import java.io.File

class PlayActivity : AppCompatActivity() {
    private var simpleExoplayer: SimpleExoPlayer? = null
    private var mediaItem: MediaItem? = null
    private var uri: Uri? = null
    lateinit var ui: ActivityPlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(ui.root)
        ui.setBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(applicationContext)) {
                    showPermissionDialog(applicationContext)
                } else {
                    setRingtone()
                }
            } else {
                setRingtone()
            }
        }
    }

    private fun setRingtone() {
        RingtoneManager.setActualDefaultRingtoneUri(
            this@PlayActivity,
            RingtoneManager.TYPE_RINGTONE, uri
        )
        if (RingtoneManager.isDefault(uri)) {
            Toast.makeText(
                this@PlayActivity,
                R.string.ringtone_installed, Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this@PlayActivity,
                R.string.wrong, Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        simpleExoplayer!!.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun initializePlayer() {
        simpleExoplayer = SimpleExoPlayer.Builder(this).build()
        ui.exoplayerView.player = simpleExoplayer
        ui.exoplayerView.setShowNextButton(false)
        val intent = intent
        if (intent != null) {
            val filePath = intent.getStringExtra("filepath")
            uri = Uri.fromFile(File(filePath))
        }
        mediaItem = MediaItem.fromUri(uri!!)
        simpleExoplayer!!.setMediaItem(mediaItem!!)
        simpleExoplayer!!.prepare()
    }

    private fun releasePlayer() {
        if (simpleExoplayer != null) {
            simpleExoplayer!!.release()
            simpleExoplayer = null
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun showPermissionDialog(context: Context) {
        val alert = AlertDialog.Builder(this@PlayActivity)
        alert.setMessage(R.string.give_perm)
        alert.setTitle(R.string.need_perm)
        alert.setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
        alert.setPositiveButton(R.string.ok) { dialogInterface: DialogInterface?, i: Int ->
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + context.applicationContext.packageName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
        alert.show()
    }
}