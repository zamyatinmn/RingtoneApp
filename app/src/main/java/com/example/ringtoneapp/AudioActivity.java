package com.example.ringtoneapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.material.snackbar.Snackbar;
import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class AudioActivity extends AppCompatActivity {
    private final int PERMISSION_CODE = 100;

    private Uri uri;
    private ImageView imageView;
    private TextView textLeftView;
    private TextView textRightView;
    private RangeSeekBar rangeSeekBar;
    private CheckBox fadeInCB;
    private CheckBox fadeOutCB;
    private Button trimBtn;
    private TextView fileName;


    private SimpleExoPlayer simpleExoplayer;


    private boolean isPlay = false;
    private int duration;
    private String filePrefix;
    private String originalPath;
    private File dest;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        Intent intent = getIntent();
        if (intent != null) {
            String filePath = intent.getStringExtra("uri");
            uri = Uri.parse(filePath);
        }
        initiateViews();
        prepareAudio();

        String temp = getRealPathFromUri(AudioActivity.this, uri);
        fileName.setText(temp.substring(temp.lastIndexOf("/") + 1));


        mediaPlayer.setOnPreparedListener(mediaPlayer -> {
            duration = mediaPlayer.getDuration() / 1000;
            textRightView.setText(getTime(duration));
            mediaPlayer.setLooping(true);
            rangeSeekBar.setRangeValues(0, duration);
            rangeSeekBar.setSelectedMinValue(0);
            rangeSeekBar.setSelectedMaxValue(duration);
            rangeSeekBar.setEnabled(true);

            rangeSeekBar.setOnRangeSeekBarChangeListener((bar, minValue, maxValue) -> {
                mediaPlayer.seekTo((int) minValue * 1000);
                textLeftView.setText(getTime((int) bar.getSelectedMinValue()));
                textRightView.setText(getTime((int) bar.getSelectedMaxValue()));
            });
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                if (mediaPlayer.getCurrentPosition() >= rangeSeekBar.getSelectedMaxValue().intValue() * 1000) {
                    mediaPlayer.seekTo(rangeSeekBar.getSelectedMinValue().intValue() * 1000);
                }
            }, 1000);
        });
        imageView.setOnClickListener(view -> {
            if (isPlay) {
                mediaPlayer.pause();
                isPlay = false;
                imageView.setImageResource(R.drawable.ic_play);
            } else {
                mediaPlayer.start();
                isPlay = true;
                imageView.setImageResource(R.drawable.ic_pause);
            }

        });
        trimBtn.setOnClickListener(view -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(AudioActivity.this);
            LinearLayout linearLayout = new LinearLayout(AudioActivity.this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(50, 0, 50, 100);
            EditText input = new EditText(AudioActivity.this);
            input.setLayoutParams(lp);
            input.setGravity(Gravity.TOP | Gravity.START);
            input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            linearLayout.addView(input, lp);

            alert.setMessage("Set audio name");
            alert.setTitle("Audio name");
            alert.setView(linearLayout);

            alert.setNegativeButton("cancel", (dialogInterface, i) -> dialogInterface.dismiss());
            alert.setPositiveButton("submit", (dialogInterface, i) -> {
                filePrefix = input.getText().toString();
                if (hasPermissions()) {
                    trim(rangeSeekBar.getSelectedMinValue().intValue() * 1000,
                            rangeSeekBar.getSelectedMaxValue().intValue() * 1000, filePrefix);
                    finish();
                    dialogInterface.dismiss();
                } else {
                    requestPermissionWithRationale();
                }
            });
            alert.show();
        });
    }

    private void prepareAudio() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(AudioActivity.this, uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initiateViews() {
        imageView = findViewById(R.id.imageView);
        textLeftView = findViewById(R.id.textLeftView);
        textRightView = findViewById(R.id.textRightView);
        rangeSeekBar = findViewById(R.id.seekBar);
        fadeInCB = findViewById(R.id.fadeInCB);
        fadeOutCB = findViewById(R.id.fadeOutCB);
        trimBtn = findViewById(R.id.trimBtn);
        fileName = findViewById(R.id.fileName);
    }

    private String getTime(int seconds) {
        int rem = seconds % 3600;
        int mn = rem / 60;
        int sec = rem % 60;
        return String.format("%02d:%02d", mn, sec);
    }

    private void trim(int start, int end, String fileName) {
        File file = new File(Environment.getExternalStorageDirectory() + "/TrimAudios");
        if (!file.exists()) {
            Log.i("Directory created:", String.valueOf(file.mkdir()));
        }
        filePrefix = fileName;
        String fileExt = ".mp3";
        dest = new File(file, filePrefix + fileExt);
        originalPath = getRealPathFromUri(getApplicationContext(), uri);
        duration = ((end - start) / 1000);

        FFmpeg.execute(prepareCommand(start));
        Toast.makeText(this, "Audio trimmed successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(AudioActivity.this, PlayActivity.class);
        intent.putExtra("filepath", dest.getAbsolutePath());
        startActivity(intent);
    }

    private String[] prepareCommand(int start) {
        List<String> commandList = new LinkedList<>();
        commandList.add("-ss");
        commandList.add("" + start / 1000);
        commandList.add("-t");
        commandList.add("" + duration);
        commandList.add("-i");
        commandList.add("" + originalPath);
        commandList.add("-acodec");
        commandList.add("libmp3lame");
        if (fadeInCB.isChecked() || fadeOutCB.isChecked()) {
            commandList.add("-af");
            String temp = "afade=t=in:st=0:d=3,afade=t=out:st=" + (duration - 3) + ":d=3";
            int begin = 0, middle = 20, end = temp.length();
            if (fadeInCB.isChecked() && !fadeOutCB.isChecked()) {
                temp = temp.substring(begin, middle - 1);
            } else if (fadeOutCB.isChecked() && !fadeInCB.isChecked()) {
                temp = temp.substring(middle, end);
            }
            commandList.add(temp);
        }
        commandList.add(dest.getAbsolutePath());

        return commandList.toArray(new String[commandList.size()]);
    }

    private String getRealPathFromUri(Context applicationContext, Uri containUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Audio.Media.DATA};
            cursor = applicationContext.getContentResolver().query(containUri, proj,
                    null, null, null);
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
    }

    private boolean hasPermissions() {
        int res;

        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        for (String perms : permissions) {
            res = checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }
        return true;
    }

    public void requestPermissionWithRationale() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            final String message = "Storage permission is required to save a new file";
            Snackbar.make(AudioActivity.this.findViewById(R.id.activity_view), message, Snackbar.LENGTH_LONG)
                    .setAction("GRANT", v -> requestPerms()).show();
        } else {
            requestPerms();
        }
    }

    private void requestPerms() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(permissions, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allowed = true;

        if (requestCode == PERMISSION_CODE) {
            for (int res : grantResults) {
                allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
            }
        } else {
            allowed = false;
        }
        if (allowed) {
            Toast.makeText(AudioActivity.this, "TADA", Toast.LENGTH_SHORT).show();
//            openFile();
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Storage Permissions denied.", Toast.LENGTH_SHORT).show();
            } else {
                showNoStoragePermissionSnackbar();
            }
        }

    }

    public void showNoStoragePermissionSnackbar() {
        Snackbar.make(AudioActivity.this.findViewById(R.id.activity_view), "Storage permission isn't granted", Snackbar.LENGTH_LONG)
                .setAction("SETTINGS", v -> {
                    openApplicationSettings();

                    Toast.makeText(getApplicationContext(),
                            "Open Permissions and grant the Storage permission",
                            Toast.LENGTH_SHORT)
                            .show();
                })
                .show();
    }

    public void openApplicationSettings() {
        Intent appSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(appSettingsIntent, PERMISSION_CODE);
    }

    private void initializePlayer() {
        simpleExoplayer = new SimpleExoPlayer.Builder(this).build();
        Intent intent = getIntent();
        if (intent != null) {
            String filePath = intent.getStringExtra("uri");
            uri = Uri.parse(filePath);
        }
        MediaItem mediaItem = MediaItem.fromUri(uri);
        simpleExoplayer.setMediaItem(mediaItem);
        simpleExoplayer.prepare();
    }

    private void releasePlayer() {
        if (simpleExoplayer != null) {
            simpleExoplayer.release();
            simpleExoplayer = null;
        }
    }
}