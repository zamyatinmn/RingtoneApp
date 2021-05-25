package com.example.ringtoneapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
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
        playerView = findViewById(R.id.exoplayerView);
        setBtn = findViewById(R.id.setBtn);
        setBtn.setOnClickListener(view -> {
            if (!Settings.System.canWrite(getApplicationContext())) {
                startManageWriteSettingsActivity(getApplicationContext());
            }

            RingtoneManager.setActualDefaultRingtoneUri(PlayActivity.this,
                    RingtoneManager.TYPE_RINGTONE, uri);
            Toast.makeText(PlayActivity.this, "Ringtone successfully installed", Toast.LENGTH_SHORT).show();
        });
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

    private static void startManageWriteSettingsActivity(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getApplicationContext().getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}