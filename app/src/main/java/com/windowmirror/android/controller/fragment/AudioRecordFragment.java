package com.windowmirror.android.controller.fragment;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import view.visualizer.AudioVisualizerView;

/**
 * A Fragment used to record and save audio.
 * Use of this fragment requires the following two permissions:
 * android.permission.RECORD_AUDIO
 * android.permission.WRITE_EXTERNAL_STORAGE
 *
 * @author alliecurry
 */
public class AudioRecordFragment extends Fragment implements AudioRecorder.AudioListener {
    public static final String TAG = AudioRecordFragment.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 0xee;
    private boolean isRecording = false;
    private ImageView recordButton;

    // File path to the most recent recording. Includes file name (eg. "storage/wm/audio.wav")
    private String audioFilePath;
    private long startTime;

    private AudioRecorder audioRecorder;
    private MediaPlayer soundEffectPlayer;
    private AudioVisualizerView visualizer;

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

    /** @return true if the recording is now set to begin */
    public boolean toggleRecording() {
        if (isRecording) {
            recordButton.setSelected(false);
            stopRecording();
            playSoundEffect(null);
            if (getActivity() instanceof RecordListener) {
                ((RecordListener) getActivity()).onRecordStop();
            }
        } else {
            playSoundEffect(onSoundEffectComplete);
        }
        isRecording = !isRecording;
        return isRecording;
    }

    private MediaPlayer.OnCompletionListener onSoundEffectComplete = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            recordButton.setSelected(true);
            audioFilePath = FileUtility.getDirectoryPath() + "/" + FileUtility.generateAudioFileName();

            if (getActivity() instanceof RecordListener) {
                ((RecordListener) getActivity()).onRecordStart();
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startRecording(audioFilePath);
                }
            }, 250);
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

    private synchronized void startRecording(final String fileName) {
        startTime = System.currentTimeMillis();
        audioRecorder = new AudioRecorder(fileName + ".wav", this);
        audioRecorder.setVisualizer(visualizer);
        audioRecorder.startRecording();
    }

    private synchronized void stopRecording() {
        try {
            audioRecorder.stopRecording();
            Log.d(TAG, "Recording created to file: " + audioFilePath);
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
        final Entry entry = createEntry(chunks);
        Intent oxfordIntent = new Intent(getActivity(), SpeechApiService.class);
        oxfordIntent.putExtra(SpeechApiService.KEY_ENTRY, entry);
        getActivity().startService(oxfordIntent);
    }

    private Entry createEntry(final List<String> chunks) {
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
}
