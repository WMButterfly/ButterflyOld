package com.windowmirror.android.listener;

import com.windowmirror.android.model.Entry;

/**
 * Interface used to perform entry-related actions.
 * @author alliecurry
 */
public interface EntryActionListener {

    void onEntryCreated(final Entry entry);
    void onEntryUpdated(final Entry entry);

}
