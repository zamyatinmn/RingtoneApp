package com.example.ringtoneapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat.*
import androidx.core.content.PermissionChecker
import com.google.android.material.snackbar.Snackbar

/**
 * @param context Activity context
 * @param permission Requested permission
 * @param texts Need four values,
 * 1 - Reason for what need the permission
 * 2 - Permission denied message
 * 3 - Permission isn't granted message
 * 4 - Please open the permission menu and issue the required permission
 * @param activityView Basic view
 */
class Perms(val context: Context, val permission: String, texts: Array<Int>, val activityView: View) {
    private val PERMISSION_CODE = 100
    private var texts: Array<Int> = arrayOf(
        R.string.reason_for_permission,
        R.string.perm_deny,
        R.string.perm_not_grant,
        R.string.open_and_grant
    )

    init {
        if (texts.size == 4) {
            this.texts = texts
        }
    }

    private fun showNoPermissionSnackbar() {
        Snackbar.make(
            activityView,
            texts[2],
            Snackbar.LENGTH_LONG
        ).setAction(R.string.settings) {
            openApplicationSettings()
            Toast.makeText(
                context,
                texts[3],
                Toast.LENGTH_SHORT
            ).show()
        }.show()
    }

    private fun openApplicationSettings() {
        val appSettingsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + context.packageName)
        )
        startActivityForResult(context as Activity, appSettingsIntent, PERMISSION_CODE, null)
    }

    fun requestPermissionWithRationale() {
        if (shouldShowRequestPermissionRationale(
                context as Activity,
                permission
//                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            val message = texts[0]
            Snackbar.make(activityView, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.grant) { v: View? -> requestPerms() }.show()
        } else {
            requestPerms()
        }
    }

    private fun requestPerms() {
        val permissions = arrayOf(permission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(context as Activity, permissions, PERMISSION_CODE)
        }
    }

    fun hasPermission(): Boolean {
        val permissions = arrayOf(permission)
        for (perms in permissions) {
            val res = PermissionChecker.checkCallingOrSelfPermission(context, perms)
            if (res != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun onRequest(requestCode: Int, grantResults: IntArray): Boolean {
        var allowed = true
        if (requestCode == PERMISSION_CODE) {
            for (res in grantResults) {
                allowed = allowed && res == PackageManager.PERMISSION_GRANTED
            }
        } else {
            allowed = false
        }
        if (allowed) {
            return true
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(
                        context as Activity, permission
                    )
                ) {
                    Toast.makeText(context, texts[1], Toast.LENGTH_SHORT).show()
                } else {
                    showNoPermissionSnackbar()
                }
            }
        }
        return false
    }
}