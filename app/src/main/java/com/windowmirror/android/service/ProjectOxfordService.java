package com.windowmirror.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.microsoft.projectoxford.speechrecognition.*;
import com.windowmirror.android.model.Entry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProjectOxfordService extends IntentService implements ISpeechRecognitionServerEvents {
    private static final String TAG = "ProjectOxfordService";
    public static final String KEY_ENTRY = "entry";

    private Entry entry;

    public ProjectOxfordService() {
        super("ProjectOxfordService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        entry = (Entry) intent.getSerializableExtra(KEY_ENTRY);
        if (entry != null) {
            processNewFile(entry.getAudioFilePath());
        } else {
            Log.e(TAG, "IntentService cannot process null Entry");
        }
    }

    public void processNewFile(String filename) {
        final DataRecognitionClient client = SpeechRecognitionServiceFactory.createDataClient(
                SpeechRecognitionMode.LongDictation,
                "en_us",
                ProjectOxfordService.this,
                "00c3a2a047cb4085831e5d1cc483af22");
        try {
            File audioFile = new File(filename);
            InputStream fileStream = new FileInputStream(audioFile);
//            File audioFile = new File("/storage/emulated/0/WindowMirror/whatstheweatherlike.wav");
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
        if (recognitionResult != null && recognitionResult.Results != null
                && recognitionResult.Results.length > 0) {
            final String transcription = recognitionResult.Results[0].DisplayText;
            Log.v(TAG, "Transcription Result: " + transcription);
            if (entry != null) {
                entry.setTranscription(transcription);
            }
            // TODO BROADCAST ENTRY UPDATED MESSAGE
        }
    }

    @Override
    public void onIntentReceived(String s) {
    }

    @Override
    public void onError(int i, String s) {
        Log.e(TAG, "Error: " + i + ": " + s);
    }

    @Override
    public void onAudioEvent(boolean b) {
        Log.v(TAG, "Audio Event: " + b);
    }
}
