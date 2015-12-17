package com.windowmirror.android.controller.fragment;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
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
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.service.ProjectOxfordService;
import com.windowmirror.android.util.FileUtility;

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

    private MediaPlayer soundEffectPlayer;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_record, container, false);
        final boolean isAccepted = checkPermissions();
        if (isAccepted) {
            // Ensure necessary directories are created
            FileUtility.makeDirectories();
        }

        // Initialize record button listener
        recordButton = (ImageView) layout.findViewById(R.id.button_record);
        recordButton.setColorFilter(getResources().getColor(R.color.royal), PorterDuff.Mode.SRC_ATOP);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleRecording();
            }
        });

        return layout;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (soundEffectPlayer != null) {
            soundEffectPlayer.release();
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

    public void toggleRecording() {
        if (isRecording) {
            recordButton.setSelected(false);
            stopRecording();
            playSoundEffect(null);
        } else {
            playSoundEffect(onSoundEffectComplete);
        }
        isRecording = !isRecording;
    }

    private MediaPlayer.OnCompletionListener onSoundEffectComplete = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            recordButton.setSelected(true);
            audioFilePath = FileUtility.getDirectoryPath() + "/" + FileUtility.generateAudioFileName();
            startRecording(audioFilePath);
        }
    };

    private void onRecordFail() {
        isRecording = false;
        recordButton.setSelected(false);
    }

    private AudioRecorder audioTest;
    private synchronized void startRecording(final String fileName) {
        startTime = System.currentTimeMillis();
        audioTest = new AudioRecorder(fileName + ".wav", this);
        audioTest.startRecording();
    }

    private synchronized void stopRecording() {
        try {
            audioTest.stopRecording();
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
    public void onAudioRecordComplete(String filePath) {
        final Entry entry = createEntry();
        Intent oxfordIntent = new Intent(getActivity(), ProjectOxfordService.class);
        oxfordIntent.putExtra(ProjectOxfordService.KEY_ENTRY, entry);
        getActivity().startService(oxfordIntent);
    }

    private Entry createEntry() {
        final Entry entry = new Entry();
        final long now = System.currentTimeMillis();
        entry.setTimestamp(now);
        entry.setDuration(now - startTime);
        entry.setAudioFilePath(audioFilePath + ".wav");
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
        }
    }
}
