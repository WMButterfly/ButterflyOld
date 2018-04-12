package com.windowmirror.android.controller.fragment;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.windowmirror.android.R;
import com.windowmirror.android.audio.AudioRecorder;
import com.windowmirror.android.listener.EntryActionListener;
import com.windowmirror.android.listener.NavigationListener;
import com.windowmirror.android.listener.RecordListener;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.model.Transcription;
import com.windowmirror.android.service.SpeechApiService;
import com.windowmirror.android.util.FileUtility;

import com.microsoft.bing.speech.Conversation;
import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import view.visualizer.AudioVisualizerView;

import static org.slf4j.MDC.put;

/**
 * A Fragment used to record and save audio.
 * Use of this fragment requires the following two permissions:
 * android.permission.RECORD_AUDIO
 * android.permission.WRITE_EXTERNAL_STORAGE
 *
 * @author alliecurry
 */
public class AudioRecordFragment extends Fragment implements AudioRecorder.AudioListener, ISpeechRecognitionServerEvents {
    public static final String TAG = AudioRecordFragment.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 0xee;
    private boolean isRecording = false;
    private ImageView recordButton;

    // File path to the most recent recording. Includes file name (eg. "storage/wm/audio.wav")
    private String audioFilePath;
    private long startTime;

    //private AudioRecorder audioRecorder;
    private MediaPlayer soundEffectPlayer;
    private AudioVisualizerView visualizer;

    DataRecognitionClient dataClient = null;
    MicrophoneRecognitionClient micClient = null;
    public enum FinalResponseStatus { NotReceived, OK, Timeout }
    FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;
    int m_waitSeconds = 0;
    final List<String> chunks = new ArrayList<>();
    String transcription = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_record, container, false);
        final boolean isAccepted = checkPermissions();
        if (isAccepted) {
            // Ensure necessary directories are created
            FileUtility.makeDirectories();
        }

        // Initialize record button listener
        recordButton = layout.findViewById(R.id.button_record);
        recordButton.setColorFilter(getResources().getColor(R.color.royal), PorterDuff.Mode.SRC_ATOP);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleRecording();
            }
        });

        visualizer = layout.findViewById(R.id.visualizer);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof NavigationListener) {
            ((NavigationListener) getActivity()).showToolbar(false);
        }
    }

    @Override
    public void onPause() {
        if (isRecording) {
            recordButton.setSelected(false);
            stopRecording();
            isRecording = false;
        }
        super.onPause();
        if (soundEffectPlayer != null) {
            soundEffectPlayer.release();
            soundEffectPlayer = null;
        }
    }

    /**
     * @return true if necessary permissions are already granted.
     */
    @TargetApi(23)
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    public synchronized void doStopRecording() {
        Log.d("butterfly", "Stopping recording...");
        recordButton.setSelected(false);
        stopRecording();
        playSoundEffect(null);
        this.isRecording = false;
        if (getActivity() instanceof RecordListener) {
            ((RecordListener) getActivity()).onRecordStop();
        }
    }

    public synchronized void doStartRecording() {
        Log.d("butterfly", "Starting recording...");
        playSoundEffect(onSoundEffectComplete);
        this.isRecording = true;
        if (getActivity() instanceof RecordListener) {
            ((RecordListener) getActivity()).onRecordStart();
        }
    }

    /** @return true if the recording is now set to begin */
    public synchronized boolean toggleRecording() {
        if (isRecording) {
            this.doStopRecording();
            //isRecording = false;
        } else {
            this.doStartRecording();
           // isRecording = true;
        }
        return isRecording;
    }

    private MediaPlayer.OnCompletionListener onSoundEffectComplete = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            recordButton.setSelected(true);
            audioFilePath = FileUtility.getDirectoryPath() + "/" + FileUtility.generateAudioFileName();
            startRecording(audioFilePath);
            /*
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                }
            }, 0);
            */
        }
    };

    private void onRecordFail() {
        isRecording = false;
        recordButton.setSelected(false);
        Toast.makeText(getContext(), "Recording failed", Toast.LENGTH_SHORT).show();
        if (getActivity() instanceof RecordListener) {
            ((RecordListener) getActivity()).onRecordStop();
        }
    }

    public boolean getIsRecording() {
        return isRecording;
    }

    private synchronized void startRecording(final String fileName) {
        startTime = System.currentTimeMillis();
        this.transcription = "";
        this.chunks.clear();
        this.startRecognition();

        //TODO: Stream audio at this point.
        /* LOCAL AUDIO RECORDING
        audioRecorder = new AudioRecorder(fileName + ".wav", this);
        audioRecorder.setVisualizer(visualizer);
        audioRecorder.startRecording();
        */
    }

    private synchronized void stopRecording() {
        try {
            this.stopRecognition();
            /*
            LOCAL AUDIO RECORDING
            audioRecorder.stopRecording();
            */
            //Log.d(TAG, "Recording created to file: " + audioFilePath);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onAudioRecordError() {
        onRecordFail();
    }

    @Override
    public void onAudioRecordComplete(String filePath, List<String> chunks) {
        Log.d ("butterfly", "onAudioRecordComplete()");
        final Entry entry = createEntry(chunks);
        //Intent oxfordIntent = new Intent(getActivity(), SpeechApiService.class);
        //oxfordIntent.putExtra(SpeechApiService.KEY_ENTRY, entry);
        //getActivity().startService(oxfordIntent);
    }

    private Entry createEntry(final List<String> chunks) {
        Log.d ("butterfly", "Creating Entry..");
        final Entry entry = new Entry();
        final long now = System.currentTimeMillis();

        entry.setTimestamp(now);
        entry.setDuration(now - startTime);

        int index = 0;
        final StringBuilder sb = new StringBuilder();
        String toAppend = null;
        for (final String chunk : chunks) {
            Matcher m = thanksButterflyPattern.matcher(chunk);
            if (m.find()) {
                this.WriteLine("removed: " + m.group(2));
                toAppend = m.group(1);
                this.WriteLine("keeping:" + toAppend);
            } else {
                toAppend = chunk;
            }
            sb.append(toAppend);
            sb.append(" ");
            this.WriteLine("["+ (index++) +"]"+toAppend);
        }
        String fullTranscription = sb.toString();
        this.WriteLine("fullTranscription: "+ fullTranscription);

        entry.setFullTranscription(fullTranscription);
        if (getActivity() instanceof EntryActionListener) {
           ((EntryActionListener) getActivity()).onEntryCreated(entry);
        }
        return entry;
    }

    private Entry createEntryOld(final List<String> chunks) {
        final Entry entry = new Entry();
        final long now = System.currentTimeMillis();
        final String filePath = audioFilePath + ".wav";
        entry.setTimestamp(now);
        entry.setDuration(now - startTime);
        entry.setAudioFilePath(filePath);
        final List<Transcription> transcriptions = new ArrayList<>();
        for (final String chunk : chunks) {
            transcriptions.add(new Transcription(chunk));
        }
        if (transcriptions.isEmpty()) { // There was an issue with chunking
            // ...use a copy of the main file as a chunk
            final String chunkFile = audioFilePath + ".chunk.wav";
            try {
                FileUtility.copyFile(new File(filePath), new File(chunkFile));
                transcriptions.add(new Transcription(chunkFile));
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
        entry.setTranscriptions(transcriptions);
        if (getActivity() instanceof EntryActionListener) {
            ((EntryActionListener) getActivity()).onEntryCreated(entry);
        }
        return entry;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission Granted");
                    FileUtility.makeDirectories();
                } else {
                    Log.d(TAG, "Permission Failed");
                    Toast.makeText(getActivity(),
                        "You must accept the record permission to use this app",
                        Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void playSoundEffect(final MediaPlayer.OnCompletionListener onComplete) {
        if (soundEffectPlayer == null) {
            soundEffectPlayer = MediaPlayer.create(getActivity(), R.raw.wmbeep);
            soundEffectPlayer.setVolume(0.25f, 0.25f);
        }

        soundEffectPlayer.setOnCompletionListener(onComplete);

        try {
            soundEffectPlayer.start();
        } catch (final IllegalStateException e) {
            Log.e(TAG, "Could not start sound effect: " + e.toString());
            soundEffectPlayer = null;
            if (onComplete != null) {
                onComplete.onCompletion(null);
            }
        }
    }

    /**
     * Gets a value indicating whether or not to use the microphone.
     * @return true if [use microphone]; otherwise, false.
     */
    private Boolean getUseMicrophone() {
        return true;
        /*
            int id = this._radioGroup.getCheckedRadioButtonId();
            return id == R.id.micIntentRadioButton ||
                id == R.id.micDictationRadioButton ||
                id == (R.id.micRadioButton - 1);
         */
    }

    /**
     * Gets a value indicating whether LUIS results are desired.
     * @return true if LUIS results are to be returned otherwise, false.
     */
    private Boolean getWantIntent() {
        return false;
        /*
        int id = this._radioGroup.getCheckedRadioButtonId();
        return id == R.id.dataShortIntentRadioButton ||
                id == R.id.micIntentRadioButton;
        */
    }

    private void startRecognition() {
        // old param was: View arg0
        //this._startButton.setEnabled(false);
        //this._radioGroup.setEnabled(false);

        ////this.m_waitSeconds = this.getMode() == SpeechRecognitionMode.ShortPhrase ? 20 : 200;
        this.m_waitSeconds =  10;
        //this.ShowMenu(false);

        this.LogRecognitionStart();

        if (this.getUseMicrophone()) {
            if (this.micClient == null) {
                if (this.getWantIntent()) {
                    this.WriteLine("--- Start microphone dictation with Intent detection ----");

                    this.micClient =
                            SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                                    getActivity(),
                                    this.getDefaultLocale(),
                                    this,
                                    this.getPrimarySubscriptionKey(),
                                    this.getLuisAppId(),
                                    this.getLuisSubscriptionID());
                }
                else
                {
                    this.micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(
                            getActivity(),
                            this.getMode(),
                            this.getDefaultLocale(),
                            this,
                            this.getPrimarySubscriptionKey());
                }

                this.micClient.setAuthenticationUri(this.getAuthenticationUri());
            }

            this.micClient.startMicAndRecognition();

        }
        else
        {
            /*
            Recognize from file

            if (null == this.dataClient) {
                if (this.getWantIntent()) {
                    this.dataClient =
                            SpeechRecognitionServiceFactory.createDataClientWithIntent(
                                    this,
                                    this.getDefaultLocale(),
                                    this,
                                    this.getPrimarySubscriptionKey(),
                                    this.getLuisAppId(),
                                    this.getLuisSubscriptionID());
                }
                else {
                    this.dataClient = SpeechRecognitionServiceFactory.createDataClient(
                            this,
                            this.getMode(),
                            this.getDefaultLocale(),
                            this,
                            this.getPrimarySubscriptionKey());
                }

                this.dataClient.setAuthenticationUri(this.getAuthenticationUri());
            }

            this.SendAudioHelper((this.getMode() == SpeechRecognitionMode.ShortPhrase) ? this.getShortWaveFile() : this.getLongWaveFile());
            */
        }
    }

    public void stopRecognition() {
        if (this.micClient != null) {
            this.micClient.endMicAndRecognition();
            /*
            try {
                this.micClient.finalize();
            } catch (Throwable throwable) {
                Log.d("butterfly", "Caught exception..");
                throwable.printStackTrace();
                StackTraceElement[] stackTraces = throwable.getStackTrace();
                for (final StackTraceElement stackTrace : stackTraces) {
                    Log.d("butterfly", stackTrace.toString());
                }
            }
            this.micClient = null;
            */
        }
    }

    /**
     * Gets the LUIS application identifier.
     * @return The LUIS application identifier.
     */
    private String getLuisAppId() {
        return this.getString(R.string.luisAppID);
    }

    /**
     * Gets the LUIS subscription identifier.
     * @return The LUIS subscription identifier.
     */
    private String getLuisSubscriptionID() {
        return this.getString(R.string.luisSubscriptionID);
    }

    /**
     * Gets the primary subscription key
     */
    public String getPrimarySubscriptionKey() {
        return this.getString(R.string.primarySubscriptionKey);
    }
    /**
     * Gets the Cognitive Service Authentication Uri.
     * @return The Cognitive Service Authentication Uri.  Empty if the global default is to be used.
     */
    private String getAuthenticationUri() {
        return "";
    }

    /**
     * Gets the current speech recognition mode.
     * @return The speech recognition mode.
     */
    private SpeechRecognitionMode getMode() {
        return SpeechRecognitionMode.LongDictation;
        /*
        int id = this._radioGroup.getCheckedRadioButtonId();
        if (id == R.id.micDictationRadioButton ||
                id == R.id.dataLongRadioButton) {
            return SpeechRecognitionMode.LongDictation;
        }

        return SpeechRecognitionMode.ShortPhrase;
        */
    }

    /**
     * Gets the default locale.
     * @return The default locale.
     */
    private String getDefaultLocale() {
        return "en-us";
    }

    /**
     * Logs the recognition start.
     */
    private void LogRecognitionStart() {
        String recoSource;
        if (this.getUseMicrophone()) {
            recoSource = "microphone";
        } else if (this.getMode() == SpeechRecognitionMode.ShortPhrase) {
            recoSource = "short wav file";
        } else {
            recoSource = "long wav file";
        }

        this.WriteLine("\n--- Start speech recognition using " + recoSource + " with " + this.getMode() + " mode in " + this.getDefaultLocale() + " language ----\n\n");
    }

    /*
    Restore this method to recognize audio in a file.

    private void SendAudioHelper(String filename) {
        RecognitionTask doDataReco = new RecognitionTask(this.dataClient, this.getMode(), filename);
        try
        {
            doDataReco.execute().get(m_waitSeconds, TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            doDataReco.cancel(true);
            isReceivedResponse = FinalResponseStatus.Timeout;
        }
    }
    */

    public void onFinalResponseReceived(final RecognitionResult response) {
        boolean isFinalDicationMessage = this.getMode() == SpeechRecognitionMode.LongDictation &&
                (response.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                        response.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);
        if (null != this.micClient && this.getUseMicrophone() && ((this.getMode() == SpeechRecognitionMode.ShortPhrase) || isFinalDicationMessage)) {
            // we got the final result, so it we can end the mic reco.  No need to do this
            // for dataReco, since we already called endAudio() on it as soon as we were done
            // sending all the data.
        }

        if (isFinalDicationMessage) {
            this.stopRecording();
            Log.d ("butterfly", "isFinalDicationMessage!!: " + this.transcription);
            ////this._startButton.setEnabled(true);
            this.isReceivedResponse = FinalResponseStatus.OK;
            this.onAudioRecordComplete("", this.chunks);
        }

        if (!isFinalDicationMessage) {
            this.WriteLine("********* Final n-BEST Results *********");
            for (int i = 0; i < response.Results.length; i++) {
                this.WriteLine("[" + i + "]" + " Confidence=" + response.Results[i].Confidence +
                        " Text=\"" + response.Results[i].DisplayText + "\"");
                this.transcription = response.Results[i].DisplayText;
            }
            this.chunks.add(this.transcription);
            this.WriteLine();
        }
    }

    /**
     * Called when a final response is received and its intent is parsed
     */
    public void onIntentReceived(final String payload) {
        this.WriteLine("--- Intent received by onIntentReceived() ---");
        this.WriteLine(payload);
        this.WriteLine();
    }

    private static final Pattern thanksButterflyPattern = Pattern.compile("(.*)?((?i)thank(s)?( )?(you)? (?i)butterfly)(.*)?");

    public void onPartialResponseReceived(final String response) {
        this.WriteLine("--- Partial result received by onPartialResponseReceived() ---");
        this.WriteLine(response);
        this.WriteLine();

        this.transcription = response;

        /*
        while (m.find()) {
            found = true;
            String text = m.group(2);
            if (text != null) {
                if (sb == null) {
                    sb = new StringBuffer(this.transcription.length());
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(text));
            }
        }
        if (found && sb != null) {
            m.appendTail(sb);
            this.transcription = sb.toString();
            this.WriteLine("trimmed to: " + this.transcription);
            this.doStopRecording();
        }
        */
        Matcher m = thanksButterflyPattern.matcher(this.transcription);
        if (m.find()) {
            this.transcription = m.group(1);
            this.doStopRecording();
        }

    }

    public void onError(final int errorCode, final String response) {
        ////this._startButton.setEnabled(true);
        this.WriteLine("--- Error received by onError() ---");
        this.WriteLine("Error code: " + SpeechClientStatus.fromInt(errorCode) + " " + errorCode);
        this.WriteLine("Error text: " + response);
        this.WriteLine();
    }

    /**
     * Called when the microphone status has changed.
     * @param recording The current recording state
     */
    public void onAudioEvent(boolean recording) {
        this.WriteLine("--- Microphone status change received by onAudioEvent() ---");
        this.WriteLine("********* Microphone status: " + recording + " *********");
        if (recording) {
            this.WriteLine("Please start speaking.");
            this.WriteLine();
        }

        //if (!recording) {
        //    this.micClient.endMicAndRecognition();
        //    ////this._startButton.setEnabled(true);
        //}
    }

    /**
     * Writes the line.
     */
    private void WriteLine() {
        this.WriteLine("");
    }

    /**
     * Writes the line.
     * @param text The line to write.
     */
    private void WriteLine(String text) {
        Log.i("butterfly", text);
        ////this._logText.append(text + "\n");
    }

    /*
     * Speech recognition with data (for example from a file or audio source).
     * The data is broken up into buffers and each buffer is sent to the Speech Recognition Service.
     * No modification is done to the buffers, so the user can apply their
     * own VAD (Voice Activation Detection) or Silence Detection
     *
     * @param dataClient
     * @param recoMode
     * @param filename
     */

    /*
    TODO:  Restore this to get speech from file
    private class RecognitionTask extends AsyncTask<Void, Void, Void> {
        DataRecognitionClient dataClient;
        SpeechRecognitionMode recoMode;
        String filename;

        RecognitionTask(DataRecognitionClient dataClient, SpeechRecognitionMode recoMode, String filename) {
            this.dataClient = dataClient;
            this.recoMode = recoMode;
            this.filename = filename;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Note for wave files, we can just send data from the file right to the server.
                // In the case you are not an audio file in wave format, and instead you have just
                // raw data (for example audio coming over bluetooth), then before sending up any
                // audio data, you must first send up an SpeechAudioFormat descriptor to describe
                // the layout and format of your raw audio data via DataRecognitionClient's sendAudioFormat() method.
                // String filename = recoMode == SpeechRecognitionMode.ShortPhrase ? "whatstheweatherlike.wav" : "batman.wav";
                InputStream fileStream = getAssets().open(filename);
                int bytesRead = 0;
                byte[] buffer = new byte[1024];

                do {
                    // Get  Audio data to send into byte buffer.
                    bytesRead = fileStream.read(buffer);

                    if (bytesRead > -1) {
                        // Send of audio data to service.
                        dataClient.sendAudio(buffer, bytesRead);
                    }
                } while (bytesRead > 0);

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            finally {
                dataClient.endAudio();
            }

            return null;
        }
    }
    */
}
