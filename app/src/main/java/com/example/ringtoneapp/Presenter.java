package com.example.ringtoneapp;

import android.os.Environment;
import android.util.Log;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class Presenter {
    private String originalPath;
    private File dest;
    private boolean fadeIn;
    private boolean fadeOut;
    private int duration;


    public String getTime(int seconds) {
        int rem = seconds % 3600;
        int mn = rem / 60;
        int sec = rem % 60;
        return String.format("%02d:%02d", mn, sec);
    }

    public int trim(int start, int end, String fileName, boolean fadeIn, boolean fadeOut, String originalPath) {
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.originalPath = originalPath;
        File file = new File(Environment.getExternalStorageDirectory() + "/TrimAudios");
        if (!file.exists()) {
            Log.i("Directory created:", String.valueOf(file.mkdir()));
        }
        String fileExt = ".mp3";
        dest = new File(file, fileName + fileExt);
        duration = ((end - start) / 1000);

        return FFmpeg.execute(prepareCommand(start));
    }

    public String getAbsolutePath(){
        return dest.getAbsolutePath();
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
        if (fadeIn || fadeOut) {
            commandList.add("-af");
            String temp = "afade=t=in:st=0:d=3,afade=t=out:st=" + (duration - 3) + ":d=3";
            int begin = 0, middle = 20, end = temp.length();
            if (fadeIn && !fadeOut) {
                temp = temp.substring(begin, middle - 1);
            } else if (!fadeIn && fadeOut) {
                temp = temp.substring(middle, end);
            }
            commandList.add(temp);
        }
        commandList.add(dest.getAbsolutePath());

        return commandList.toArray(new String[commandList.size()]);
    }
}
