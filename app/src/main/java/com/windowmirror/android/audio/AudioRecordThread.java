package com.windowmirror.android.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.*;

/**
 * @author alliecurry
 */
public class AudioRecordThread extends Thread {
    private static final String TAG = AudioRecordThread.class.getSimpleName();

    public static final int SAMPLE_RATE = 8000;
    public static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_FORMAT);

    final String filePath;
    private boolean stop;

    public AudioRecordThread(final String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        Log.v(TAG, "Starting recording…");

        byte audioData[] = new byte[BUFFER_SIZE];
        AudioRecord recorder = new AudioRecord(AUDIO_SOURCE,
                SAMPLE_RATE, CHANNEL_IN,
                AUDIO_FORMAT, BUFFER_SIZE);
        recorder.startRecording();

        BufferedOutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(filePath + ".raw"));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found for recording ", e);
            return;
        }

        while (!stop) {
            int status = recorder.read(audioData, 0, audioData.length);

            if (status == AudioRecord.ERROR_INVALID_OPERATION ||
                    status == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Error reading audio data!");
                return;
            }

            try {
                os.write(audioData, 0, audioData.length);
            } catch (IOException e) {
                Log.e(TAG, "Error saving recording ", e);
                return;
            }
        }

        try {
            os.close();

            recorder.stop();
            recorder.release();

            Log.v(TAG, "Recording done…");
            stop = false;

        } catch (IOException e) {
            Log.e(TAG, "Error when releasing", e);
        }

        try {
            WaveConverter.rawToWav(
                    new File(filePath + ".raw"),
                    new File(filePath + ".wav"),
                    SAMPLE_RATE
            );
        } catch (final Exception e) {
            Log.e(TAG, "Error converting to wav", e);
        }
    }

    public synchronized void stopRecording() {
        stop = true;
    }

}
