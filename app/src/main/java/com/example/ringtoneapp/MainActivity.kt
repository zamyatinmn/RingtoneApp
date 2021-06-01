package com.example.ringtoneapp

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.ringtoneapp.databinding.ActivityMainBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private val PERMISSION_CODE = 100
    private val REQUEST_CODE = 10
    private var isPlay = false
    private var duration = 0
    private var chosenAudioUri: Uri? = null
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private val presenter = Trimmer()
    private var isFirstStarted = false
    lateinit var ui: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)
        ui.openFileBtn.setOnClickListener {
            if (hasPermissions()) {
                openFile()
            } else {
                requestPermissionWithRationale()
            }
        }
    }

    private fun openFile() {
        releasePlayer()
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            chosenAudioUri = data!!.data
            val audioName = getRealPathFromUri(this@MainActivity, chosenAudioUri)
            ui.fileName.text = audioName.substring(audioName.lastIndexOf("/") + 1)
            initializePlayer()
            simpleExoPlayer!!.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY && !isFirstStarted) {
                        isFirstStarted = true
                        duration = simpleExoPlayer!!.duration.toInt() / 1000
                        ui.rightTextView.text = presenter.getTime(duration)
                        ui.seekBar.setRangeValues(0, duration)
                        ui.seekBar.selectedMinValue = 0
                        ui.seekBar.selectedMaxValue = duration
                        ui.seekBar.isEnabled = true
                        ui.seekBar.setOnRangeSeekBarChangeListener { bar, minValue, maxValue ->
                            simpleExoPlayer!!.seekTo((minValue as Int * 1000).toLong())
                            ui.leftTextView.text = presenter.getTime(bar.selectedMinValue.toInt())
                            ui.rightTextView.text = presenter.getTime(bar.selectedMaxValue.toInt())
                        }
                        val handler = Handler(Looper.getMainLooper())
                        handler.postDelayed({
                            if (simpleExoPlayer!!.currentPosition >= ui.seekBar.selectedMaxValue.toInt() * 1000) {
                                simpleExoPlayer!!.seekTo((ui.seekBar.selectedMinValue.toLong() * 1000))
                            }
                        }, 1000)
                    }
                }
            })
            ui.imageView.setOnClickListener {
                if (isPlay) {
                    simpleExoPlayer!!.pause()
                    isPlay = false
                    ui.imageView.setImageResource(R.drawable.ic_play)
                } else {
                    simpleExoPlayer!!.play()
                    isPlay = true
                    ui.imageView.setImageResource(R.drawable.ic_pause)
                }
            }
            ui.trimBtn.setOnClickListener {
                simpleExoPlayer!!.stop()
                val alert = AlertDialog.Builder(this@MainActivity)
                val linearLayout = LinearLayout(this@MainActivity)
                linearLayout.orientation = LinearLayout.VERTICAL
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(50, 0, 50, 100)
                val input = EditText(this@MainActivity)
                input.layoutParams = lp
                input.gravity = Gravity.TOP or Gravity.START
                input.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                linearLayout.addView(input, lp)
                alert.setMessage(R.string.set_audio_name)
                alert.setTitle(R.string.audio_name)
                alert.setView(linearLayout)
                alert.setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
                alert.setPositiveButton(R.string.submit) { dialogInterface: DialogInterface, i: Int ->
                    val filePrefix = input.text.toString()
                    val result = presenter.trim(
                        ui.seekBar.selectedMinValue.toInt() * 1000,
                        ui.seekBar.selectedMaxValue.toInt() * 1000, filePrefix,
                        ui.fadeInCB.isChecked, ui.fadeOutCB.isChecked,
                        getRealPathFromUri(applicationContext, chosenAudioUri)
                    )
                    when (result) {
                        0 -> {
                            Toast.makeText(this, R.string.trim_success, Toast.LENGTH_SHORT)
                                .show()
                            val intent = Intent(this@MainActivity, PlayActivity::class.java)
                            intent.putExtra("filepath", presenter.absolutePath)
                            startActivity(intent)
                        }
                        255 -> Toast.makeText(
                            this,
                            R.string.cancel_process,
                            Toast.LENGTH_SHORT
                        ).show()
                        else -> Toast.makeText(
                            this,
                            R.string.error_process + result,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    finish()
                    dialogInterface.dismiss()
                }
                alert.show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
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

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun hasPermissions(): Boolean {
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        for (perms in permissions) {
            val res = checkCallingOrSelfPermission(perms)
            if (res != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermissionWithRationale() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            val message = R.string.reason_for_permission
            Snackbar.make(findViewById(R.id.activityView), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.grant) { v: View? -> requestPerms() }.show()
        } else {
            requestPerms()
        }
    }

    private fun requestPerms() {
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        var allowed = true
        if (requestCode == PERMISSION_CODE) {
            for (res in grantResults) {
                allowed = allowed && res == PackageManager.PERMISSION_GRANTED
            }
        } else {
            allowed = false
        }
        if (allowed) {
            openFile()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, R.string.perm_deny, Toast.LENGTH_SHORT).show()
                } else {
                    showNoStoragePermissionSnackbar()
                }
            }
        }
    }

    private fun showNoStoragePermissionSnackbar() {
        Snackbar.make(
            ui.activityView,
            R.string.perm_not_grant,
            Snackbar.LENGTH_LONG
        ).setAction(R.string.settings) {
            openApplicationSettings()
                Toast.makeText(
                    applicationContext,
                    R.string.open_and_grant,
                    Toast.LENGTH_SHORT
                ).show()
            }.show()
    }

    private fun openApplicationSettings() {
        val appSettingsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(appSettingsIntent, PERMISSION_CODE)
    }

    private fun initializePlayer() {
        simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
        val mediaItem = MediaItem.fromUri(
            chosenAudioUri!!
        )
        simpleExoPlayer!!.setMediaItem(mediaItem)
        simpleExoPlayer!!.prepare()
    }

    private fun releasePlayer() {
        if (simpleExoPlayer != null) {
            isFirstStarted = false
            simpleExoPlayer!!.release()
            simpleExoPlayer = null
        }
    }
}