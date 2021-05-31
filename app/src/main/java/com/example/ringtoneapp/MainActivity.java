package com.example.ringtoneapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.material.snackbar.Snackbar;

import org.florescu.android.rangeseekbar.RangeSeekBar;


public class MainActivity extends AppCompatActivity {
    private final int PERMISSION_CODE = 100;
    private final int REQUEST_CODE = 10;

    private Button openBtn;
    private ImageView imageView;
    private TextView textLeftView;
    private TextView textRightView;
    private RangeSeekBar<Integer> rangeSeekBar;
    private CheckBox fadeInCB;
    private CheckBox fadeOutCB;
    private Button trimBtn;
    private TextView fileName;

    private boolean isPlay = false;
    private int duration;

    private Uri chosenAudioUri;
    private SimpleExoPlayer simpleExoPlayer;
    private final Presenter presenter = new Presenter();
    private boolean isFirstStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initiateViews();
        openBtn.setOnClickListener(view -> {
            if (hasPermissions()) {
                openFile();
            } else {
                requestPermissionWithRationale();
            }
        });
    }

    private void initiateViews() {
        imageView = findViewById(R.id.image_view);
        textLeftView = findViewById(R.id.left_text_view);
        textRightView = findViewById(R.id.right_text_view);
        rangeSeekBar = findViewById(R.id.seek_bar);
        fadeInCB = findViewById(R.id.fade_in_CB);
        fadeOutCB = findViewById(R.id.fade_out_CB);
        trimBtn = findViewById(R.id.trim_btn);
        fileName = findViewById(R.id.file_name);
        openBtn = findViewById(R.id.open_file_btn);
    }

    private void openFile() {
        releasePlayer();
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            chosenAudioUri = data.getData();
            String audioName = getRealPathFromUri(MainActivity.this, chosenAudioUri);
            fileName.setText(audioName.substring(audioName.lastIndexOf("/") + 1));
            initializePlayer();

            simpleExoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY && !isFirstStarted) {
                        isFirstStarted = true;
                        duration = (int) simpleExoPlayer.getDuration() / 1000;
                        textRightView.setText(presenter.getTime(duration));
                        rangeSeekBar.setRangeValues(0, duration);
                        rangeSeekBar.setSelectedMinValue(0);
                        rangeSeekBar.setSelectedMaxValue(duration);
                        rangeSeekBar.setEnabled(true);

                        rangeSeekBar.setOnRangeSeekBarChangeListener((bar, minValue, maxValue) -> {
                            simpleExoPlayer.seekTo(minValue * 1000);
                            textLeftView.setText(presenter.getTime((int) bar.getSelectedMinValue()));
                            textRightView.setText(presenter.getTime((int) bar.getSelectedMaxValue()));
                        });
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            if (simpleExoPlayer.getCurrentPosition() >= rangeSeekBar.getSelectedMaxValue() * 1000) {
                                simpleExoPlayer.seekTo(rangeSeekBar.getSelectedMinValue() * 1000);
                            }
                        }, 1000);
                    }
                }
            });
            imageView.setOnClickListener(view -> {
                if (isPlay) {
                    simpleExoPlayer.pause();
//                    mediaPlayer.pause();
                    isPlay = false;
                    imageView.setImageResource(R.drawable.ic_play);
                } else {
                    simpleExoPlayer.play();
//                    mediaPlayer.start();
                    isPlay = true;
                    imageView.setImageResource(R.drawable.ic_pause);
                }

            });
            trimBtn.setOnClickListener(view -> {
                simpleExoPlayer.stop();
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                LinearLayout linearLayout = new LinearLayout(MainActivity.this);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(50, 0, 50, 100);
                EditText input = new EditText(MainActivity.this);
                input.setLayoutParams(lp);
                input.setGravity(Gravity.TOP | Gravity.START);
                input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                linearLayout.addView(input, lp);

                alert.setMessage("Set audio name");
                alert.setTitle("Audio name");
                alert.setView(linearLayout);

                alert.setNegativeButton("cancel", (dialogInterface, i) -> dialogInterface.dismiss());
                alert.setPositiveButton("submit", (dialogInterface, i) -> {
                    String filePrefix = input.getText().toString();
                    int result = presenter.trim(rangeSeekBar.getSelectedMinValue() * 1000,
                            rangeSeekBar.getSelectedMaxValue() * 1000, filePrefix,
                            fadeInCB.isChecked(), fadeOutCB.isChecked(),
                            getRealPathFromUri(getApplicationContext(), chosenAudioUri));
                    switch (result) {
                        case 0:
                            Toast.makeText(this, "Audio trimmed successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                            intent.putExtra("filepath", presenter.getAbsolutePath());
                            startActivity(intent);
                            break;
                        case 255:
                            Toast.makeText(this, "The process was canceled by user", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Toast.makeText(this, "The process was terminated with an error " + result, Toast.LENGTH_SHORT).show();
                            break;
                    }
                    finish();
                    dialogInterface.dismiss();
                });
                alert.show();
            });
        }
        super.onActivityResult(requestCode, resultCode, data);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private boolean hasPermissions() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        for (String perms : permissions) {
            int res = checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }
        return true;
    }

    public void requestPermissionWithRationale() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            final String message = "Storage permission is required to open and save a new file";
            Snackbar.make(MainActivity.this.findViewById(R.id.activity_view), message, Snackbar.LENGTH_LONG)
                    .setAction("GRANT", v -> requestPerms()).show();
        } else {
            requestPerms();
        }
    }

    private void requestPerms() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_CODE);
        }
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
            openFile();
        } else {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Storage Permissions denied.", Toast.LENGTH_SHORT).show();
                } else {
                    showNoStoragePermissionSnackbar();
                }
            }
        }

    }

    public void showNoStoragePermissionSnackbar() {
        Snackbar.make(MainActivity.this.findViewById(R.id.activity_view), "Storage permission isn't granted", Snackbar.LENGTH_LONG)
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
        simpleExoPlayer = new SimpleExoPlayer.Builder(this).build();
        MediaItem mediaItem = MediaItem.fromUri(chosenAudioUri);
        simpleExoPlayer.setMediaItem(mediaItem);
        simpleExoPlayer.prepare();
    }

    private void releasePlayer() {
        if (simpleExoPlayer != null) {
            isFirstStarted = false;
            simpleExoPlayer.release();
            simpleExoPlayer = null;
        }
    }
}