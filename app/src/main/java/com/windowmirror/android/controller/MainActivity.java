package com.windowmirror.android.controller;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.windowmirror.android.R;

/**
 * The Activity that starts on app launch.
 * @author alliecurry
 */
public class MainActivity extends FragmentActivity {
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
}
