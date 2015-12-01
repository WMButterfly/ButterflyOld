package com.windowmirror.android.controller.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.windowmirror.android.R;
import com.windowmirror.android.controller.adapter.HistoryAdapter;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.util.LocalPrefs;

/**
 * Fragment used to display and play previous recordings.
 * @author alliecurry
 */
public class HistoryListFragment extends Fragment implements HistoryAdapter.Listener {

    private HistoryAdapter adapter;
    private Entry playingEntry;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_history, container, false);
        adapter = new HistoryAdapter(getActivity(), this);
        adapter.addAll(LocalPrefs.getStoredEntries(getActivity()));
        if (layout instanceof ListView) {
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
        // TODO pause current audio (if any)
        if (isEntryPlaying(entry)) {
            playingEntry = null;
            adapter.notifyDataSetChanged();
        } else {
            playingEntry = entry;
            adapter.notifyDataSetChanged();
            // TODO play new audio
        }
    }

    @Override
    public boolean isEntryPlaying(final Entry entry) {
        return playingEntry != null && playingEntry.equals(entry);
    }
}
