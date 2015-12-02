package com.windowmirror.android.service;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.util.Log;
import com.microsoft.projectoxford.speechrecognition.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProjectOxfordService extends IntentService implements ISpeechRecognitionServerEvents {
    private static final String TAG = "ProjectOxfordService";

    DataRecognitionClient client;

    public ProjectOxfordService() {
        super("ProjectOxfordService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        processNewFile(intent.getStringExtra("filename"));
    }

    @Override
    public void onCreate() {
        client = SpeechRecognitionServiceFactory.createDataClient(
            SpeechRecognitionMode.LongDictation,
            "en_us",
            this,
            "00c3a2a047cb4085831e5d1cc483af22");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        processNewFile(intent.getStringExtra("filename"));
        return Service.START_NOT_STICKY;
    }

    public void processNewFile(String filename) {
        try {
            File audioFile = new File(filename);
//            File audioFile = new File("/storage/emulated/0/WindowMirror/whatstheweatherlike.wav");
            InputStream fileStream = new FileInputStream(audioFile);
//            InputStream fileStream = getAssets().open("whatstheweatherlike.wav");
            int bytesRead = 0;
            byte[] buffer = new byte[1024];

            do {
                // Get  Audio data to send into byte buffer.
                bytesRead = fileStream.read(buffer);

                if (bytesRead > -1) {
                    // Send of audio data to service.
                    client.sendAudio(buffer, bytesRead);
                }
            } while (bytesRead > 0);
        } catch (IOException ex) {
            Log.e(TAG, ex.toString());
        } finally {
            client.endAudio();
        }

    }

    @Override
    public void onPartialResponseReceived(String s) {

    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
        Log.v(TAG, "Response received!");
    }

    @Override
    public void onIntentReceived(String s) {

    }

    @Override
    public void onError(int i, String s) {

    }

    @Override
    public void onAudioEvent(boolean b) {

    }
}
