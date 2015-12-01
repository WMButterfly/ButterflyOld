package com.windowmirror.android.controller;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import com.windowmirror.android.R;
import com.windowmirror.android.controller.fragment.HistoryListFragment;
import com.windowmirror.android.listener.EntryActionListener;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.util.LocalPrefs;

/**
 * The Activity that starts on app launch.
 * @author alliecurry
 */
public class MainActivity extends FragmentActivity implements EntryActionListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();
        } else { // No more fragments... used to avoid blank screen
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ensure Entries are stored before going to background
        LocalPrefs.storeEntries(this);
    }

    @Override
    public void onEntryCreated(Entry entry) {
        LocalPrefs.addEntry(this, entry);
        final Fragment fragmentBottom = getSupportFragmentManager().findFragmentById(R.id.fragment_bottom);
        if (fragmentBottom instanceof HistoryListFragment) {
            ((HistoryListFragment) fragmentBottom).addEntry(entry);
        }
    }

    @Override
    public void onEntryUpdated(Entry entry) {
        final Fragment fragmentBottom = getSupportFragmentManager().findFragmentById(R.id.fragment_bottom);
        if (fragmentBottom instanceof HistoryListFragment) {
            ((HistoryListFragment) fragmentBottom).notifyDataSetChanged();
        }
    }
}
