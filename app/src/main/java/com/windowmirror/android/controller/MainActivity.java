package com.windowmirror.android.controller;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.*;
import com.windowmirror.android.R;
import com.windowmirror.android.controller.dialog.SettingsDialog;
import com.windowmirror.android.controller.fragment.AudioRecordFragment;
import com.windowmirror.android.controller.fragment.HistoryListFragment;
import com.windowmirror.android.listener.EntryActionListener;
import com.windowmirror.android.listener.RecordListener;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.model.OxfordStatus;
import com.windowmirror.android.service.BootReceiver;
import com.windowmirror.android.service.ProjectOxfordService;
import com.windowmirror.android.service.SphynxService;
import com.windowmirror.android.util.LocalPrefs;
import com.windowmirror.android.util.NetworkUtility;

import static com.windowmirror.android.service.ProjectOxfordService.KEY_ENTRY;

/**
 * The Activity that starts on app launch.
 * @author alliecurry
 */
public class MainActivity extends FragmentActivity implements EntryActionListener, View.OnTouchListener,
        RecordListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MAX_TAP_COUNT = 6;
    private static final int MAX_ENTRY_RETRY_QUEUE = 5;
    private int tapCount = 0;
    private long touchDownMs = 0;

    private Intent sphynxIntent;
    private int prevOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerBroadcastReceivers();
        findViewById(R.id.fragment_top).setOnTouchListener(this);
    }

    private void registerBroadcastReceivers() {
        // Project Oxford Broadcasts
        final IntentFilter intentFilterOxford = new IntentFilter(ProjectOxfordService.ACTION_ENTRY_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(new EntryBroadcastReceiver(), intentFilterOxford);

        // Sphynx Broadcasts
        final SphynxBroadcastReceiver sphynxBroadcastReceiver = new SphynxBroadcastReceiver();
        final IntentFilter intentFilterSphynxStart = new IntentFilter(SphynxService.ACTION_START);
        final IntentFilter intentFilterSphynxStop = new IntentFilter(SphynxService.ACTION_STOP);

        LocalBroadcastManager.getInstance(this).registerReceiver(sphynxBroadcastReceiver, intentFilterSphynxStart);
        LocalBroadcastManager.getInstance(this).registerReceiver(sphynxBroadcastReceiver, intentFilterSphynxStop);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO For Play Store: add Privacy Terms and have user accept them before starting Service
        // TODO When the above is added, you may want to set default value for background service to "false" in LocalPrefs
        BootReceiver.enable(this);

        final Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(SphynxService.KEY_START)) {
            Log.d(TAG, "Starting recording from Intent");
            getIntent().removeExtra(SphynxService.KEY_START);
            toggleRecording();
        } else {
            startSphynxService();
        }

        queueEntriesForRetry();
    }

    private void toggleRecording() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_top);
        if (fragment instanceof AudioRecordFragment) {
            final boolean isRecording = ((AudioRecordFragment) fragment).toggleRecording();
            if (!isRecording) { // We don't want to start the service if we just began recording
                startSphynxService();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ensure Entries are stored before going to background
        LocalPrefs.storeEntries(this);

        // Stop or start sphynx service depending on settings
        if (LocalPrefs.getIsBackgroundService(this)) {
            startSphynxService();
        } else {
            stopSphynxService();
        }
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

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if ((System.currentTimeMillis() - touchDownMs) > ViewConfiguration.getTapTimeout()) {
                    // Too long between taps... reset count
                    tapCount = 0;
                    break;
                }

                tapCount++;

                if (tapCount == MAX_TAP_COUNT) {
                    tapCount = 0;
                    showServiceSettings();
                }
            case MotionEvent.ACTION_DOWN:
                touchDownMs = System.currentTimeMillis();
                break;
        }
        return true;
    }

    private void showServiceSettings() {
        final SettingsDialog dialog = new SettingsDialog();
        dialog.show(getSupportFragmentManager(), SettingsDialog.TAG);
    }

    @Override
    public void onRecordStart() {
        stopSphynxService();
        lockOrientation();
    }

    @Override
    public void onRecordStop() {
        startSphynxService();
        unlockOrientation();
    }

    private void startSphynxService() {
        if (!isServiceRunning(this)) {
            startService(sphynxIntent = new Intent(getApplicationContext(), SphynxService.class));
        }
    }

    private void stopSphynxService() {
        if (sphynxIntent != null) {
            stopService(sphynxIntent);
        } else { // Need to create an Intent...
            stopService(new Intent(getApplicationContext(), SphynxService.class));
        }
    }

    private class EntryBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Entry entry = (Entry) intent.getSerializableExtra(KEY_ENTRY);
            if (entry != null) {
                onEntryUpdated(entry);
            }
        }
    }

    private class SphynxBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            toggleRecording();
        }
    }

    private static boolean isServiceRunning(final Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SphynxService.class.getName().equals(service.service.getClassName())) {
                Log.d(TAG, ">> SphynxService is already running");
                return true;
            }
        }
        return false;
    }

    /** Allows the user to rotate their screen */
    private void unlockOrientation() {
        setRequestedOrientation(prevOrientation);
    }

    /** Prevents the user from rotating their screen and restarting the Activity. */
    private void lockOrientation() {
        prevOrientation = getRequestedOrientation();
        Display display = getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();
        final Point size = new Point();
        display.getSize(size);
        int height = size.y;
        int width = size.x;

        switch (rotation) {
            case Surface.ROTATION_90:
                if (width > height) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                }
                break;
            case Surface.ROTATION_180:
                if (height > width) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
                break;
            case Surface.ROTATION_270:
                if (width > height) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                break;
            default :
                if (height > width) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                break;
        }
    }

    private void queueEntriesForRetry() {
        if (!NetworkUtility.isWifiConnected(this)) {
            return; // Only queue entries on Wifi
        }
        int count = 0;
        for (final Entry entry : LocalPrefs.getStoredEntries(this)) {
            if (entry.getOxfordStatus() == OxfordStatus.REQUIRES_RETRY ||
                    (entry.getOxfordStatus() == OxfordStatus.NONE && entry.getTranscription() == null ||
                            entry.getTranscription().isEmpty()) ||
                    (entry.getOxfordStatus() == OxfordStatus.PENDING
                            && System.currentTimeMillis() - entry.getTimestamp() > 300000)) {
                sendEntryToOxford(entry);
                ++count;
            }
            if (count == MAX_ENTRY_RETRY_QUEUE) {
                return;
            }
        }
    }

    private void sendEntryToOxford(final Entry entry) {
        Log.d(TAG, "Sending Entry to Oxford: " + entry.getTimestamp());
        final Intent oxfordIntent = new Intent(this, ProjectOxfordService.class);
        oxfordIntent.putExtra(ProjectOxfordService.KEY_ENTRY, entry);
        startService(oxfordIntent);
    }
}
