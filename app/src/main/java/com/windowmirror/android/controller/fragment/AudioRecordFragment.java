package com.windowmirror.android.controller.fragment;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.windowmirror.android.R;
import com.windowmirror.android.audio.AudioRecordThread;
import com.windowmirror.android.audio.AudioTest;
import com.windowmirror.android.audio.AudioRecorderV2;
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
public class AudioRecordFragment extends Fragment {
    public static final String TAG = AudioRecordFragment.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 0xee;
    private static final int AUDIO_BIT_RATE = 256000;
    private static final int AUDIO_SAMPLE_RATE = 16000;
    private boolean isRecording = false;
    private TextView recordButton;
    private MediaRecorder mediaRecorder = null;

    // File path to the most recent recording. Includes file name (eg. "storage/wm/audio.wav")
    private String audioFilePath;
    private long startTime;

    private AudioRecordThread audioRecordThread;

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
        recordButton = (TextView) layout.findViewById(R.id.button_record);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRecordClick();
            }
        });


        return layout;
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

    private void onRecordClick() {
        if (isRecording) {
            recordButton.setText(R.string.record_start);
            stopRecording();
        } else {
            recordButton.setText(R.string.record_stop);
            audioFilePath = FileUtility.getDirectoryPath() + "/" + FileUtility.generateAudioFileName();
            startRecording(audioFilePath);
        }
        isRecording = !isRecording;
    }

    private void onRecordFail() {
        isRecording = false;
        recordButton.setText(R.string.record_start);
    }

    private AudioRecorderV2 audioTest;
    private synchronized void startRecording(final String fileName) {
        audioRecordThread = new AudioRecordThread(fileName, onRecordComplete);
        startTime = System.currentTimeMillis();
//        audioRecordThread.start();
        audioTest = new AudioRecorderV2(fileName + ".wav");
        audioTest.startRecording();
    }

    private synchronized void stopRecording() {
//        audioRecordThread.stopRecording();
        try {
            audioTest.stopRecording(onRecordComplete);
            createEntry();
            Log.d(TAG, "Recording created to file: " + audioFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AudioRecordThread.OnCompleteListener onRecordComplete = new AudioRecordThread.OnCompleteListener() {
        @Override
        public void onComplete(String filePath) {
            Intent oxfordIntent = new Intent(getActivity(), ProjectOxfordService.class);
            oxfordIntent.putExtra("filename", filePath);
            getActivity().startService(oxfordIntent);
        }
    };

    private void createEntry() {
        final Entry entry = new Entry();
        final long now = System.currentTimeMillis();
        entry.setTimestamp(now);
        entry.setDuration(now - startTime);
        entry.setAudioFilePath(audioFilePath + ".wav");
        if (getActivity() instanceof EntryActionListener) {
            ((EntryActionListener) getActivity()).onEntryCreated(entry);
        }
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
}
