package com.example.ringtoneapp

import android.os.Environment
import android.util.Log
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.util.*

class Trimmer {
    private lateinit var originalPath: String
    private lateinit var dest: File
    private var fadeIn = false
    private var fadeOut = false
    private var duration = 0

    fun getTime(seconds: Int): String {
        val rem = seconds % 3600
        val mn = rem / 60
        val sec = rem % 60
        return String.format("%02d:%02d", mn, sec)
    }

    fun trim(
        start: Int,
        end: Int,
        fileName: String,
        fadeIn: Boolean,
        fadeOut: Boolean,
        originalPath: String
    ): Int {
        this.fadeIn = fadeIn
        this.fadeOut = fadeOut
        this.originalPath = originalPath
        val file = File(Environment.getExternalStorageDirectory().toString() + "/TrimAudios")
        if (!file.exists()) {
            Log.i("Directory created:", file.mkdir().toString())
        }
        val fileExt = ".mp3"
        dest = File(file, fileName + fileExt)
        duration = (end - start) / 1000
        return FFmpeg.execute(prepareCommand(start))
    }

    val absolutePath: String
        get() = dest.absolutePath

    private fun prepareCommand(start: Int): Array<String> {
        val commandList: MutableList<String> = LinkedList()
        commandList.add("-ss")
        commandList.add("" + start / 1000)
        commandList.add("-t")
        commandList.add("" + duration)
        commandList.add("-i")
        commandList.add("" + originalPath)
        commandList.add("-acodec")
        commandList.add("libmp3lame")
        if (fadeIn || fadeOut) {
            commandList.add("-af")
            var temp = "afade=t=in:st=0:d=3,afade=t=out:st=" + (duration - 3) + ":d=3"
            val begin = 0
            val middle = 20
            val end = temp.length
            if (fadeIn && !fadeOut) {
                temp = temp.substring(begin, middle - 1)
            } else if (!fadeIn && fadeOut) {
                temp = temp.substring(middle, end)
            }
            commandList.add(temp)
        }
        commandList.add(dest.absolutePath)
        return commandList.toTypedArray()
    }
}