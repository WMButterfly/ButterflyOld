package com.windowmirror.android.controller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.result.Credentials;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import com.google.android.gms.actions.NoteIntents;
import com.windowmirror.android.R;
import com.windowmirror.android.auth.AuthActivity;
import com.windowmirror.android.service.BackendService;
import io.fabric.sdk.android.Fabric;

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
        Fabric.with(this, new Crashlytics(), new CrashlyticsNdk());
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAuth();
            }
        }, TIMEOUT_MS);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        Log.d ("butterfly", "inside onNewIntent!");
        //Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (intent != null) {
            Log.d ("butterfly", "started with intent. Action=" + action + " Type="+type + ".");
            if (NoteIntents.ACTION_CREATE_NOTE.equals(action)) {
                Log.d ("butterfly", "started with intent to record.");
                //showRecordingFragment(true);
            }
        } else {
            Log.d ("butterfly", "started without intent.");
        }
    }

    private void checkAuth() {
        if (BackendService.hasCredentials(this)) {
            BackendService.getCredentialsManager(this).getCredentials(new BaseCallback<Credentials, CredentialsManagerException>() {
                @Override
                public void onSuccess(Credentials payload) {
                    BackendService.setCredentials(SplashActivity.this, payload);
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }

                @Override
                public void onFailure(CredentialsManagerException error) {
                    startActivity(new Intent(SplashActivity.this, AuthActivity.class));
                }
            });
        } else {
            startActivity(new Intent(this, AuthActivity.class));
        }
        finish();
    }
}
