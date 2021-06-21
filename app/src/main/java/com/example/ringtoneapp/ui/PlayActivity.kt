package com.example.ringtoneapp.ui

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.ringtoneapp.App
import com.example.ringtoneapp.Perms
import com.example.ringtoneapp.R
import com.example.ringtoneapp.databinding.ActivityPlayBinding
import com.example.ringtoneapp.presenters.PlayPresenter
import javax.inject.Inject


class PlayActivity : AppCompatActivity() {
    private lateinit var ui: ActivityPlayBinding

    @Inject
    lateinit var presenter: PlayPresenter

    private lateinit var perms: Perms

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityPlayBinding.inflate(layoutInflater)

        (applicationContext as App).appComponent.inject(this)
        presenter.bindContext(this)

        setContentView(ui.root)

        perms = Perms(
            this, Manifest.permission.WRITE_CONTACTS, arrayOf(
                R.string.reason_for_permission_contact,
                R.string.perm_deny_contact,
                R.string.perm_not_grant_contact,
                R.string.open_and_grant_contact
            ), ui.activityView
        )


        ui.exoplayerView.player = presenter.init()
        ui.exoplayerView.setShowNextButton(false)

        ui.setBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(applicationContext)) {
                    showPermissionDialog(applicationContext)
                } else {
                    presenter.setRingtone()
                }
            } else {
                presenter.setRingtone()
            }
        }

        ui.setContactBtn.setOnClickListener {
            if (perms.hasPermission()) {
                pickContact()
            } else {
                perms.requestPermissionWithRationale()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (perms.onRequest(requestCode, grantResults)) {
            pickContact()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (data != null) {
                data.data?.let { presenter.setContactRingtone(it) }
            }
        }
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.data = ContactsContract.Contacts.CONTENT_URI
        ActivityCompat.startActivityForResult(this, intent, 1, null)
    }

    override fun onResume() {
        super.onResume()
        this.presenter.play()
    }

    override fun onPause() {
        super.onPause()
        this.presenter.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.presenter.release()
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