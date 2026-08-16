package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var recordFile: File? = null

    fun startRecording(): File? {
        try {
            // Create temporary file inside the cache directory (no permanent raw audio storage)
            val cacheDir = context.cacheDir
            recordFile = File.createTempFile("audio_note_", ".m4a", cacheDir)

            @Suppress("DEPRECATION")
            val recordDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            recorder = recordDevice.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(recordFile?.absolutePath)
                prepare()
                start()
            }
            Log.d("AudioRecorder", "Recording started: ${recordFile?.absolutePath}")
            return recordFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            stopRecording()
            return null
        }
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            val finishedFile = recordFile
            Log.d("AudioRecorder", "Recording stopped: ${finishedFile?.absolutePath}")
            finishedFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recording", e)
            recorder?.release()
            recorder = null
            null
        }
    }

    fun cleanup() {
        try {
            stopRecording()
            recordFile?.delete()
            recordFile = null
        } catch (e: Exception) {
            // silent catch
        }
    }
}
