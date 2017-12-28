package com.windowmirror.android.controller.fragment;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import com.windowmirror.android.R;
import com.windowmirror.android.controller.adapter.HistoryAdapter;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.util.LocalPrefs;

/**
 * Fragment used to display and play previous recordings.
 * @author alliecurry
 */
public class HistoryListFragment extends Fragment implements HistoryAdapter.Listener {
    public static final String TAG = HistoryListFragment.class.getSimpleName();
    private HistoryAdapter adapter;

    private MediaPlayer mediaPlayer;
    private Entry playingEntry;
    private boolean isAudioPlaying = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_history, container, false);
        adapter = new HistoryAdapter(getActivity(), this);
        adapter.addAll(LocalPrefs.getStoredEntries(getActivity()));
        if (layout instanceof ListView) {
            final View header = inflater.inflate(R.layout.history_header, (ViewGroup) layout, false);
            ((ListView) layout).addHeaderView(header);
            ((ListView) layout).setHeaderDividersEnabled(true);
            ((ListView) layout).setAdapter(adapter);
        }
        return layout;
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void addEntry(final Entry entry) {
        if (adapter != null) {
            adapter.insert(entry, 0);
        }
    }

    @Override
    public void onPausePlay(final Entry entry) {
        stopAudio();
        if (isEntryPlaying(entry)) {
            playingEntry = null;
            adapter.notifyDataSetChanged();
        } else {
            playingEntry = entry;
            adapter.notifyDataSetChanged();
            playAudio(entry.getAudioFilePath());
        }
    }

    @Override
    public boolean isEntryPlaying(final Entry entry) {
        return playingEntry != null && playingEntry.equals(entry);
    }

    /** Stops any currently playing audio. */
    private void stopAudio() {
        if (mediaPlayer != null) {
            try {
                Log.v(TAG, "Stopping current music...");
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (final IllegalStateException e) {
                Log.e(TAG, "Could not stop media: " + e.toString());
            }
            isAudioPlaying = false;
        }
    }

    /** Plays an audio clip based on the given path. */
    private void playAudio(final String path) {
        isAudioPlaying = true;
        try {
            final Uri uri = Uri.parse(path);
            mediaPlayer = MediaPlayer.create(getActivity(), uri);
            mediaPlayer.setOnCompletionListener(onAudioComplete);
            mediaPlayer.start();
        } catch (final Exception e) {
            Log.e(TAG, "Could not start audio: " + e.toString());
            Toast.makeText(getActivity(), "Error starting media. " +
                    "Please check your connection.", Toast.LENGTH_SHORT)
                    .show();
            isAudioPlaying = false;
        }
    }

    private MediaPlayer.OnCompletionListener onAudioComplete = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            isAudioPlaying = false;
            playingEntry = null;
            adapter.notifyDataSetChanged();
        }
    };
}
