package com.windowmirror.android.controller;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.windowmirror.android.R;
import com.windowmirror.android.controller.dialog.SettingsDialog;
import com.windowmirror.android.controller.fragment.AudioRecordFragment;
import com.windowmirror.android.controller.fragment.HistoryListFragment;
import com.windowmirror.android.listener.EntryActionListener;
import com.windowmirror.android.listener.RecordListener;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.service.BootReceiver;
import com.windowmirror.android.service.ProjectOxfordService;
import com.windowmirror.android.service.SphynxService;
import com.windowmirror.android.util.LocalPrefs;

import static com.windowmirror.android.service.ProjectOxfordService.KEY_ENTRY;

/**
 * The Activity that starts on app launch.
 * @author alliecurry
 */
public class MainActivity extends FragmentActivity implements EntryActionListener, View.OnTouchListener,
        RecordListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MAX_TAP_COUNT = 6;
    private int tapCount = 0;
    private long touchDownMs = 0;

    private Intent sphynxIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final IntentFilter intentFilter = new IntentFilter(ProjectOxfordService.ACTION_ENTRY_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(new EntryBroadcastReceiver(), intentFilter);
        findViewById(R.id.fragment_bottom).setOnTouchListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(SphynxService.KEY_START)) {
            Log.d(TAG, "Starting recording from Intent");
            final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_top);
            if (fragment instanceof AudioRecordFragment) {
                ((AudioRecordFragment) fragment).toggleRecording();
                return;
            }
        }

        // TODO For Play Store: add Privacy Terms and have user accept them before starting Service
        // TODO When the above is added, you may want to set default value for background service to "false" in LocalPrefs
        BootReceiver.enable(this);
        startSphynxService();
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
            stopSphynxSerivce();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("allie", ">>>> ON NEW INTENT\n" + intent.getExtras());
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
        stopSphynxSerivce();
    }

    @Override
    public void onRecordStop() {
        startSphynxService();
    }

    private void startSphynxService() {
        if (!isServiceRunning(this)) {
            startService(sphynxIntent = new Intent(getApplicationContext(), SphynxService.class));
        }
    }

    private void stopSphynxSerivce() {
        if (sphynxIntent != null) {
            stopService(sphynxIntent);
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
}
