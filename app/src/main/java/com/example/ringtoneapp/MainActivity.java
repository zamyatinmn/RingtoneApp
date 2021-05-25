package com.example.ringtoneapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    Button openBtn;
    Uri chosenAudioUri;
    TextView textView;

    private final int REQUEST_CODE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initiateViews();

        openBtn.setOnClickListener(view -> openFile());


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            chosenAudioUri = data.getData();
            Intent intent = new Intent(MainActivity.this, AudioActivity.class);
            intent.putExtra("uri", chosenAudioUri.toString());
//            intent.putExtra("fileName", chosenAudioUri.getEncodedPath());
//            Log.i("DEBUG", chosenAudioUri.get);
            startActivity(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initiateViews() {
        openBtn = findViewById(R.id.openFileBtn);
        textView = findViewById(R.id.textView);
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE);
    }


    // TODO: 22.05.2021 запилить прогрессбар
    // TODO: 22.05.2021 медиаплеер в экзоплеер
    // TODO: 18.05.2021 добавить возможность установить на рингтон по умолчанию
}