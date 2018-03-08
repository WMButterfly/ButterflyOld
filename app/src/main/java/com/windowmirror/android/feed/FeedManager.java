package com.windowmirror.android.feed;

import android.content.Context;
import android.support.annotation.NonNull;

import com.windowmirror.android.model.Entry;
import com.windowmirror.android.model.service.Recording;
import com.windowmirror.android.util.LocalPrefs;

import java.util.ArrayList;
import java.util.List;

public final class FeedManager {
    private static FeedManager sInstance;
    private Context mContext;
    private List<Entry> mEntries;
    
    
    private FeedManager(@NonNull Context context) {
        mContext = context;
        mEntries = LocalPrefs.getStoredEntries(context);
    }
    
    public static FeedManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new FeedManager(context);
        }
        return sInstance;
    }

    public List<Entry> getAllEntries() {
        return mEntries;
    }

    public void storeEntries() {
        if (mEntries != null) {
            LocalPrefs.storeEntries(mContext, mEntries);
        }
    }

    public void addEntry(@NonNull Entry entry) {
        if (mEntries == null) {
            mEntries = LocalPrefs.getStoredEntries(mContext);
        }
        mEntries.add(0, entry);
    }

    public void updateEntry(@NonNull Entry entry) {
        if (mEntries == null) {
            mEntries = LocalPrefs.getStoredEntries(mContext);
        }

        boolean updated = false;
        for (final Entry e : mEntries) {
            if (entry.getTimestamp() == e.getTimestamp()) {
                e.setTranscriptions(entry.getTranscriptions());
                e.setRecording(entry.getRecording());
                updated = true;
                break;
            }
        }
        if (!updated) {
            // Entry was not found ... add as new entry
            mEntries.add(0, entry);
        }
    }

    /**
     * Converts a list of Recordings from a server to local Entries.
     * Either finds the local Entry already stored (so that Speech API statuses are properly matched)
     * or if the Entry doesn't exist, creates one that is simple marked "complete" as far as the Speech API is concerned.
     * Entries that are created here don't need to be stored locally and are mostly just a wrapper for the Recording data.
     * Simplification of this would be for backend to manage Speech API tasks <> Recording.
     */
    @NonNull
    public List<Entry> getEntriesForRecordings(@NonNull List<Recording> recordingsList) {
        List<Entry> userEntries = new ArrayList<>(); // Entries that match the given recordings list
        List<Entry> localEntries = getAllEntries(); // All locally stored entries
        for (Recording recording : recordingsList) {
            boolean found = false;
            for (Entry entry : localEntries) {
                if (matches(entry, recording)) {
                    entry.setRecording(recording);
                    userEntries.add(entry);
                    found = true;
                    break;
                }
            }
            if (!found) {
                userEntries.add(Entry.fromRecording(recording));
            }
        }
        return userEntries;
    }

    private static boolean matches(@NonNull Entry entry, @NonNull Recording recording) {
        if (entry.getRecording() != null && entry.getRecording().getUuid().equals(recording.getUuid())) {
            return true;
        } else if (entry.getUuid() != null && entry.getUuid().equals(recording.getUuid())) {
            return true;
        } else if (entry.getTimestamp() == recording.getTimestamp()) {
            return true;
        }
        return false;
    }
}
