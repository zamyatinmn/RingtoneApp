package com.example.ringtoneapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

import java.io.File;

public class PlayActivity extends AppCompatActivity {
    SimpleExoPlayer simpleExoplayer;
    PlayerView playerView;
    MediaItem mediaItem;
    Uri uri;
    Button setBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        initiateViews();
        setBtn.setOnClickListener(view -> {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(getApplicationContext())) {
                    showPermissionDialog(getApplicationContext());
                } else {
                    setRingtone();
                }
            } else {
                setRingtone();
            }

        });
    }

    private void setRingtone(){
        RingtoneManager.setActualDefaultRingtoneUri(PlayActivity.this,
                RingtoneManager.TYPE_RINGTONE, uri);
        if (RingtoneManager.isDefault(uri)) {
            Toast.makeText(PlayActivity.this,
                    "Ringtone successfully installed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(PlayActivity.this,
                    "Something wrong..", Toast.LENGTH_SHORT).show();
        }
    }

    private void initiateViews() {
        playerView = findViewById(R.id.exoplayer_view);
        setBtn = findViewById(R.id.set_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        simpleExoplayer.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void initializePlayer() {
        simpleExoplayer = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(simpleExoplayer);
        playerView.setShowNextButton(false);
        Intent intent = getIntent();
        if (intent != null) {
            String filePath = intent.getStringExtra("filepath");
            uri = Uri.fromFile(new File(filePath));
        }
        mediaItem = MediaItem.fromUri(uri);
        simpleExoplayer.setMediaItem(mediaItem);
        simpleExoplayer.prepare();
    }

    private void releasePlayer() {
        if (simpleExoplayer != null) {
            simpleExoplayer.release();
            simpleExoplayer = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private  void showPermissionDialog(final Context context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(PlayActivity.this);
        alert.setMessage("Please give the permission to set ringtone.");
        alert.setTitle("Need permission");
        alert.setNegativeButton("cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        alert.setPositiveButton("OK", (dialogInterface, i) -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getApplicationContext().getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
        alert.show();
    }

}