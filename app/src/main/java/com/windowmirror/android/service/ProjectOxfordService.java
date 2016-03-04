package com.windowmirror.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.microsoft.projectoxford.speechrecognition.*;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.model.OxfordStatus;
import com.windowmirror.android.model.Transcription;
import com.windowmirror.android.util.LocalPrefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProjectOxfordService extends IntentService implements ISpeechRecognitionServerEvents {
    private static final String TAG = "ProjectOxfordService";
    public static final String ACTION_ENTRY_UPDATED = "wmeupdate";
    public static final String KEY_ENTRY = "entry";

    private Entry entry;

    public ProjectOxfordService() {
        super("ProjectOxfordService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        entry = (Entry) intent.getSerializableExtra(KEY_ENTRY);
        if (entry != null && entry.getTranscriptions() != null) {
            entry.setOxfordTimestamp(System.currentTimeMillis());

            for (final Transcription transcription : entry.getTranscriptions()) {
                if (transcription.getOxfordStatus() == OxfordStatus.SUCCESSFUL
                        || transcription.getOxfordStatus() == OxfordStatus.FAILED) {
                    continue; // We only want to send new transcriptions or those marked for retry
                }

                transcription.setOxfordStatus(OxfordStatus.PENDING);
                transcription.incrementOxfordTries();

                final boolean success = processNewFile(transcription.getFilePath(),
                        getListenerForTranscription(transcription));

                if (!success) {
                    transcription.setOxfordStatus(OxfordStatus.FAILED);
                    broadcastEntry();
                }
            }
        } else {
            Log.e(TAG, "IntentService cannot process null Entry");
        }
    }

    public synchronized boolean processNewFile(final String filename,
                                            final ISpeechRecognitionServerEvents listener) {
        final DataRecognitionClient client = SpeechRecognitionServiceFactory.createDataClient(
                SpeechRecognitionMode.LongDictation,
                "en_us",
                listener,
                "00c3a2a047cb4085831e5d1cc483af22");
        try {
            File audioFile = new File(filename);

            if (!audioFile.exists() || audioFile.length() == 0) {
                return false;
            }

            InputStream fileStream = new FileInputStream(audioFile);
            int bytesRead;
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
            return false;
        } finally {
            client.endAudio();
        }
        return true;
    }

    @Override
    public void onPartialResponseReceived(String s) {
    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
    }

    private ISpeechRecognitionServerEvents getListenerForTranscription(final Transcription transcription) {
        return new ISpeechRecognitionServerEvents() {
            @Override
            public void onFinalResponseReceived(RecognitionResult recognitionResult) {
                Log.v(TAG, "Response received!");
                if (recognitionResult != null && recognitionResult.Results != null
                        && recognitionResult.Results.length > 0) {
                    final String text = recognitionResult.Results[0].DisplayText;
                    Log.v(TAG, "Transcription Result: " + text);
                    if (text != null) {
                        String oldTranscription = transcription.getText();
                        if (oldTranscription == null) {
                            oldTranscription = "";
                        } else { // Adding a space between new and old transcription.
                            oldTranscription += " ";
                        }
                        final String fullTranscription = oldTranscription + text;
                        if (fullTranscription.trim().isEmpty()) {
                            transcription.updateForEmptyTranscription();
                        } else {
                            transcription.setOxfordStatus(OxfordStatus.SUCCESSFUL);
                            deleteChunkForTranscription(transcription);
                        }
                        transcription.setText(fullTranscription);
                    }
                }
                transcription.updateForEmptyTranscription();
                broadcastEntry();
            }

            @Override
            public void onPartialResponseReceived(String s) {
            }

            @Override
            public void onIntentReceived(String s) {
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "Error: " + i + ": " + s);
                transcription.updateForEmptyTranscription();
                broadcastEntry();
            }

            @Override
            public void onAudioEvent(boolean b) {
                Log.v(TAG, "Audio Event: " + b);
            }
        };
    }

    private void deleteChunkForTranscription(final Transcription transcription) {
        final File chunkFile = new File(transcription.getFilePath());
        chunkFile.delete();
    }

    private void broadcastEntry() {
        if (entry != null) {
            LocalPrefs.updateEntry(this, entry);
            final Intent localIntent =  new Intent(ACTION_ENTRY_UPDATED).putExtra(KEY_ENTRY, entry);
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        }
    }

    @Override
    public void onIntentReceived(String s) {
    }

    @Override
    public void onError(int i, String s) {
        Log.e(TAG, "Error: " + i + ": " + s);
        broadcastEntry();
    }

    @Override
    public void onAudioEvent(boolean b) {
        Log.v(TAG, "Audio Event: " + b);
    }
}
