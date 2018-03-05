package com.windowmirror.android.controller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.windowmirror.android.R;
import com.windowmirror.android.auth.AuthActivity;

/**
 * Activity used to determine whether the user has authentication.
 */
public final class SplashActivity extends Activity {
    /**
     * Minimum length of time the splash screen will display.
     */
    private static final long TIMEOUT_MS = 1250;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAuth();
            }
        }, TIMEOUT_MS);
    }

    private void checkAuth() {
        boolean isAuthenticated = false; // TODO
        if (isAuthenticated) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, AuthActivity.class));
        }
    }
}
