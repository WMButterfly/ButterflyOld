package com.windowmirror.android.service;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.util.Log;
import com.microsoft.projectoxford.speechrecognition.*;

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
            // Note for wave files, we can just send data from the file right to the server.
            // In the case you are not an audio file in wave format, and instead you have just
            // raw data (for example audio coming over bluetooth), then before sending up any
            // audio data, you must first send up an SpeechAudioFormat descriptor to describe
            // the layout and format of your raw audio data via DataRecognitionClient's sendAudioFormat() method.
            InputStream fileStream = getAssets().open(filename);
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
            Contract.fail();
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
