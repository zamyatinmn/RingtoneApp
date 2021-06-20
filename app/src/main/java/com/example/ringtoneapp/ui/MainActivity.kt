package com.example.ringtoneapp.ui

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ringtoneapp.App
import com.example.ringtoneapp.Perms
import com.example.ringtoneapp.R
import com.example.ringtoneapp.databinding.ActivityMainBinding
import com.example.ringtoneapp.presenters.IMainPresenter
import com.google.android.exoplayer2.Player
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE = 10
    private var isPlay = false
    private var duration = 0
    private lateinit var chosenAudioUri: Uri
    lateinit var ui: ActivityMainBinding
    lateinit var perms: Perms
    @Inject
    lateinit var presenter: IMainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        (applicationContext as App).appComponent.inject(this)
        setContentView(ui.root)

        perms = Perms(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            , arrayOf(
                R.string.reason_for_permission,
                R.string.perm_deny,
                R.string.perm_not_grant,
                R.string.open_and_grant
            ), ui.activityView)

        ui.openFileBtn.setOnClickListener {
            if (perms.hasPermission()) {
                openFile()
            } else {
                perms.requestPermissionWithRationale()
            }
        }
    }

    private fun openFile() {
        if (isPlay) {
            presenter.pause()
        }
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onPause() {
        super.onPause()
        if (presenter.isFirstStarted) {
            presenter.pause()
            isPlay = false
            ui.imageView.setImageResource(R.drawable.ic_play)
        }
    }

    override fun onResume() {
        super.onResume()
        if (presenter.isFirstStarted) {
            presenter.play()
            isPlay = true
            ui.imageView.setImageResource(R.drawable.ic_pause)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE && data != null) {
            if (presenter.isFirstStarted) {
                presenter.release()
            }
            chosenAudioUri = data.data!!
            presenter.bindContext(this@MainActivity)
            val player = presenter.init(chosenAudioUri)
            ui.fileName.text = presenter.getAudioName()
            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY && !presenter.isFirstStarted) {
                        presenter.isFirstStarted = true
                        duration = player.duration.toInt() / 1000
                        ui.rightTextView.text = presenter.getTime(duration)
                        ui.seekBar.setRangeValues(0, duration)
                        ui.seekBar.selectedMinValue = 0
                        ui.seekBar.selectedMaxValue = duration
                        ui.seekBar.isEnabled = true
                        ui.seekBar.setOnRangeSeekBarChangeListener { bar, minValue, maxValue ->
                            player.seekTo((minValue as Int * 1000).toLong())
                            ui.leftTextView.text = presenter.getTime(bar.selectedMinValue.toInt())
                            ui.rightTextView.text = presenter.getTime(bar.selectedMaxValue.toInt())
                        }
                        val handler = Handler(Looper.getMainLooper())
                        handler.postDelayed({
                            if (player.currentPosition >= ui.seekBar.selectedMaxValue.toInt() * 1000) {
                                player.seekTo((ui.seekBar.selectedMinValue.toLong() * 1000))
                            }
                        }, 1000)
                    }
                }
            })
            ui.imageView.setOnClickListener {
                if (isPlay) {
                    presenter.pause()
                    isPlay = false
                    ui.imageView.setImageResource(R.drawable.ic_play)
                } else {
                    presenter.play()
                    isPlay = true
                    ui.imageView.setImageResource(R.drawable.ic_pause)
                }
            }
            ui.trimBtn.setOnClickListener {
                presenter.stop()
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
                        ui.fadeInCB.isChecked, ui.fadeOutCB.isChecked
                    )
                    when (result) {
                        0 -> {
                            Toast.makeText(this, R.string.trim_success, Toast.LENGTH_SHORT)
                                .show()
                            val intent = Intent(this@MainActivity, PlayActivity::class.java)
                            intent.putExtra("filepath", presenter.getAbsolutePath())
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

    override fun onDestroy() {
        super.onDestroy()
        presenter.release()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (perms.onRequest(requestCode, grantResults)) {
            openFile()
        }

    }


}