package com.windowmirror.android.listener;

import android.support.annotation.NonNull;

import com.windowmirror.android.model.Entry;

import view.navigation.ButterflyToolbar;

public interface NavigationListener {
    void showToolbar(boolean show);

    void setToolbarState(@NonNull ButterflyToolbar.State state);

    void onSignOut();

    void showEntryDetail(@NonNull Entry entry);
}
