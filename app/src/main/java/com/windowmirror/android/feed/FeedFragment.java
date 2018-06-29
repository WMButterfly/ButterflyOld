package com.windowmirror.android.feed;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.windowmirror.android.R;
import com.windowmirror.android.listener.NavigationListener;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.model.service.Recording;
import com.windowmirror.android.service.BackendApi;
import com.windowmirror.android.service.BackendApiCallback;
import com.windowmirror.android.service.BackendService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import view.SpacesItemDecoration;
import view.navigation.ButterflyToolbar;

/**
 * Fragment used to display and play previous recordings.
 *
 * @author alliecurry
 */
public class FeedFragment extends Fragment implements FeedAdapter.Listener, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {
    public static final String TAG = FeedFragment.class.getSimpleName();
    private FeedAdapter adapter;
    private List<Entry> entries;

    private MediaPlayer mediaPlayer;
    private Entry playingEntry;
    private boolean isAudioPlaying = false;
    private RecyclerView recyclerView;
    private CoordinatorLayout coordinatorLayout;
    private View progressSpinner;
    private View emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_history, container, false);
        adapter = new FeedAdapter(this);
        recyclerView = layout.findViewById(R.id.recycler_view);
        progressSpinner = layout.findViewById(R.id.progress);
        emptyView = layout.findViewById(R.id.empty_message);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new SpacesItemDecoration(getResources()
                .getDimensionPixelSize(R.dimen.list_item_padding)));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        if (entries != null && !entries.isEmpty()) {
            adapter.setEntries(entries);
            emptyView.setVisibility(View.GONE);
            showProgress(false);
        } else {
            loadRecordings();
        }
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof NavigationListener) {
            ((NavigationListener) getActivity()).showToolbar(true);
            ((NavigationListener) getActivity()).setToolbarState(ButterflyToolbar.State.DEFAULT);
        }
    }

    private void showProgress(boolean show) {
        progressSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Loads the Recordings from the backend API.
     */
    private void loadRecordings() {
        showProgress(true);
        BackendService.getInstance()
                .getApi()
                .getAllRecordings()
                .enqueue(new BackendApiCallback<List<Recording>>() {
                    @Override
                    public void onSuccess(@NonNull List<Recording> data) {
                        onLoadRecordingsSuccess(data);
                    }

                    @Override
                    public void onError(@Nullable String error) {
                        Log.e(TAG, "Error retrieving recordings: " + error);
                        // TODO add error state to UI?
                        emptyView.setVisibility(View.VISIBLE);
                        showProgress(false);
                    }

                    @Override
                    public Context getContext() {
                        return FeedFragment.this.getContext();
                    }

                    @Override
                    public void onAuthenticationLost() {
                        if (getActivity() instanceof NavigationListener) {
                            ((NavigationListener) getActivity()).onSignOut();
                        }
                    }
                });
    }

    // Delete patch request
    public void deleteRequestPatch(String name, final Recording recording) {
        showProgress(true);
        BackendService
                .getInstance()
                .getApi()
                .updateRecording(name, recording)
                .enqueue(new Callback<Recording>() {
                    @Override
                    public void onResponse(Call<Recording> call, Response<Recording> response) {
                        showProgress(false);
                        recording.setRecycle(false);
                        Log.e(TAG, "Delete patch request successful");
                    }

                    @Override
                    public void onFailure(Call<Recording> call, Throwable t) {
                        showProgress(false);
                        Log.e(TAG, "Delete patch request failed");
                    }
                });

    }

    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof FeedAdapter.ViewHolder) {
            // get the removed item name to display it in snack bar
            final String name = entries.get(viewHolder.getAdapterPosition()).getUuid();
            final Recording recording = entries.get(viewHolder.getAdapterPosition()).getRecording();
            // remove the item from recycler view
            deleteRequestPatch(name, recording);
            adapter.removeEntry(viewHolder.getAdapterPosition());

        }
    }

    private void onLoadRecordingsSuccess(@NonNull List<Recording> recordings) {
        if (getContext() == null) {
            return; // user already exited
        }
        entries = FeedManager.getInstance(getContext()).getEntriesForRecordings(recordings);
        if (entries.isEmpty()) { // TODO REMOVE THIS. SHOWING ALL LOCAL DATA WHEN THERE IS NO OTHER DATA
            // Doing this while trying to debug create recording not working
            entries = FeedManager.getInstance(getContext()).getAllEntries();
        }
        adapter.setEntries(entries);
        showProgress(false);
        emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public synchronized void addEntry(@NonNull Entry entry) {
        if (adapter != null) {
            adapter.addEntry(entry);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPausePlay(@NonNull final Entry entry) {
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
    public boolean isEntryPlaying(@NonNull final Entry entry) {
        return playingEntry != null && playingEntry.equals(entry);
    }

    @Override
    public void onEntrySelected(@NonNull Entry entry) {
        if (getActivity() instanceof NavigationListener) {
            ((NavigationListener) getActivity()).showEntryDetail(entry);
        }
    }

    /**
     * Stops any currently playing audio.
     */
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

    /**
     * Plays an audio clip based on the given path.
     */
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
