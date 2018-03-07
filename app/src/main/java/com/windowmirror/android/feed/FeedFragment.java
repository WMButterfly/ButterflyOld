package com.windowmirror.android.feed;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.windowmirror.android.R;
import com.windowmirror.android.listener.NavigationListener;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.util.LocalPrefs;

import java.util.ArrayList;

/**
 * Fragment used to display and play previous recordings.
 * @author alliecurry
 */
public class FeedFragment extends Fragment implements FeedAdapter.Listener {
    public static final String TAG = FeedFragment.class.getSimpleName();
    private FeedAdapter adapter;

    private MediaPlayer mediaPlayer;
    private Entry playingEntry;
    private boolean isAudioPlaying = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_history, container, false);
        adapter = new FeedAdapter(this);
        adapter.setEntries(new ArrayList<>(LocalPrefs.getStoredEntries(getActivity())));
        if (layout instanceof RecyclerView) {
            ((RecyclerView) layout).setLayoutManager(new LinearLayoutManager(getContext()));
            ((RecyclerView) layout).setAdapter(adapter);
        }
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof NavigationListener) {
            ((NavigationListener) getActivity()).showToolbar(true);
        }
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void addEntry(final Entry entry) {
        if (adapter != null) {
            adapter.addEntry(entry);
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
