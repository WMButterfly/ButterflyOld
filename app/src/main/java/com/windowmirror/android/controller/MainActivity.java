package com.windowmirror.android.controller;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.gms.common.api.GoogleApiClient;
import com.windowmirror.android.BuildConfig;
import com.windowmirror.android.R;
import com.windowmirror.android.auth.AuthActivity;
import com.windowmirror.android.controller.fragment.AudioRecordFragment;
import com.windowmirror.android.feed.FeedFragment;
import com.windowmirror.android.feed.FeedManager;
import com.windowmirror.android.feed.detail.FeedDetailFragment;
import com.windowmirror.android.listener.EntryActionListener;
import com.windowmirror.android.listener.NavigationListener;
import com.windowmirror.android.listener.RecordListener;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.model.OxfordStatus;
import com.windowmirror.android.model.Transcription;
import com.windowmirror.android.model.service.Recording;
import com.windowmirror.android.service.BackendApiCallback;
import com.windowmirror.android.service.BackendService;
/* commented out to disable always listening
import com.windowmirror.android.service.BootReceiver;
end disable */
import com.windowmirror.android.service.SpeechApiService;
/* commented out to disable always listening
import com.windowmirror.android.service.SphynxService;
end disable */
import com.windowmirror.android.util.FileUtility;
import com.windowmirror.android.util.LocalPrefs;
import com.windowmirror.android.util.NetworkUtility;
import net.danlew.android.joda.JodaTimeAndroid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import view.navigation.ButterflyToolbar;
import view.navigation.UserHeaderView;
import com.google.android.gms.actions.NoteIntents;
//import com.google.android.gms.common.api.GoogleApiClient;

import static com.windowmirror.android.service.SpeechApiService.KEY_ENTRY;

/**
 * The Activity which controls all views for authenticated users
 *
 * @author alliecurry
 */
public class MainActivity extends FragmentActivity implements
        NavigationListener,
        EntryActionListener,
        RecordListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MAX_ENTRY_RETRY_QUEUE = 5;

    private Intent sphynxIntent;
    private int prevOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

    /* Views */
    private ButterflyToolbar toolbar;
    private DrawerLayout drawerLayout;
    private UserHeaderView userHeaderView;
    private View audioRecordContainer;
    private View progressSpinner;

    //GoogleApiClient mClient;

    public static void dumpIntent(Intent i){
        if (i == null) {
            return;
        }
        Bundle bundle = i.getExtras();
        if (bundle == null) {
            return;
        }
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            Log.d(TAG, String.format("%s %s (%s)", key,
                    value.toString(), value.getClass().getName()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioRecordContainer = findViewById(R.id.audio_fragment_container);
        progressSpinner = findViewById(R.id.progress);
        registerBroadcastReceivers();
        JodaTimeAndroid.init(this);
        setupNavigation();
        showFeedFragment();

        findViewById(R.id.sign_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSignOut();
            }
        });
        findViewById(R.id.about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAboutClick();
            }
        });
        progressSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // consume clicks
            }
        });
        Log.d ("butterfly", "inside onCreate!");
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (intent != null) {
            Log.d ("butterfly", "started with intent. Action=" + action + " Type="+type + ".");
            dumpIntent(intent);
            if (NoteIntents.ACTION_CREATE_NOTE.equals(action)) {
                Log.d ("butterfly", "started with intent to record!!");
                String data = intent.getDataString();
                Log.d("butterfly", "data: "+ data);
                final Entry entry = this.createEntry(data);
                //showRecordingFragment(true);
                Log.d ("butterfly", "created entry.  entry.getFullTranscription():" + entry.getFullTranscription());

                this.CreateEntry(entry);
                //showRecordingFragment(true);
            }
        } else {
            Log.d ("butterfly", "started without intent.");
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        Log.d ("butterfly", "inside onNewIntent");
        String action = intent.getAction();
        String type = intent.getType();
        if (intent != null) {
            Log.d ("butterfly", "started with intent. Action=" + action + " Type="+type + ".");
            if (NoteIntents.ACTION_CREATE_NOTE.equals(action)) {
                Log.d ("butterfly", "started with intent to record!");
                final Entry entry = this.createEntry("test entry!");
                //showRecordingFragment(true);
                Log.d ("butterfly", "created entry.  entry.getFullTranscription():" + entry.getFullTranscription());
            }
        } else {
            Log.d ("butterfly", "started without intent.");
        }
    }

    //TODO:  Remove this copy paste uglyness!
    private Entry createEntry(String fullTranscription) {
        Log.d ("butterfly", "Creating Entry: "+ fullTranscription);
        final Entry entry = new Entry();
        final long now = System.currentTimeMillis();

        entry.setTimestamp(now);
        entry.setDuration(0);

        this.WriteLine("fullTranscription: "+ fullTranscription);

        entry.setFullTranscription(fullTranscription);
        //if (getActivity() instanceof EntryActionListener) {
        //    ((EntryActionListener) getActivity()).onEntryCreated(entry);
        //}
        return entry;
    }

    private void WriteLine(String text) {
        Log.i("butterfly", text);
        ////this._logText.append(text + "\n");
    }


    private void setupNavigation() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        userHeaderView = findViewById(R.id.user_header);
        // TODO populate header with user data?
        toolbar.setLogoListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecording();
            }
        });
        toolbar.setMenuListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavDrawer();
            }
        });
        toolbar.setBackListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                toolbar,
                R.string.app_name,
                R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
    }

    private void registerBroadcastReceivers() {
        // Speech API Broadcasts
        final IntentFilter intentFilterOxford = new IntentFilter(SpeechApiService.ACTION_ENTRY_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(new EntryBroadcastReceiver(), intentFilterOxford);

        /* commented out to disable always listening
        // Sphynx Broadcasts
        final SphynxBroadcastReceiver sphynxBroadcastReceiver = new SphynxBroadcastReceiver();
        final IntentFilter intentFilterSphynxStart = new IntentFilter(SphynxService.ACTION_START);
        final IntentFilter intentFilterSphynxStop = new IntentFilter(SphynxService.ACTION_STOP);

        LocalBroadcastManager.getInstance(this).registerReceiver(sphynxBroadcastReceiver, intentFilterSphynxStart);
        LocalBroadcastManager.getInstance(this).registerReceiver(sphynxBroadcastReceiver, intentFilterSphynxStop);
        end disable */
    }

    /* commented out to disable always listening.
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
    end disable */

    @Override
    protected void onPause() {
        super.onPause();
        // Ensure Entries are stored before going to background
        FeedManager.getInstance(this).storeEntries();

        /* commented out to remove always listening feature
        // Stop or start sphynx service depending on settings
        if (LocalPrefs.getIsBackgroundService(this)) {
            startSphynxService();
        } else {
            stopSphynxService();
        }
        end disable */
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        if (isShowingRecord) {
            toggleRecording();
            return;
        }
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();
        } else { // No more fragments... used to avoid blank screen
            finish();
        }
    }

    private void toggleNavDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private synchronized void toggleRecording() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.audio_fragment);
        if (fragment instanceof AudioRecordFragment) {
            final boolean isRecording = ((AudioRecordFragment) fragment).toggleRecording();
            showRecordingFragment(isRecording);
            if (!isRecording) { // We don't want to start the service if we just began recording
                /* commented out while disabling always listening.
                // we don't want to listen in a service while the app is in the foreground.
                startSphynxService();
                end disable */
            }
        }
    }

    private void showFeedFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FeedFragment.TAG);
        if (!(fragment instanceof FeedFragment)) {
            fragment = new FeedFragment();
        }
        replaceFragment(fragment, FeedFragment.TAG, true);
    }

    private boolean isShowingRecord;

    private synchronized void showRecordingFragment(boolean show) {
        isShowingRecord = show;
        audioRecordContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @UiThread
    private synchronized void showProgressSpinner(boolean show) {
        progressSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEntryCreated(final Entry entry) {
        this.CreateEntry(entry);
    }

    public void CreateEntry(final Entry entry) {
        Log.d ("butterfly", "EntryCreate.");

        FeedManager.getInstance(this).addEntry(entry);
        final Fragment fragment = getFragmentInView();
        if (fragment instanceof FeedFragment) {
            ((FeedFragment) fragment).addEntry(entry);
        }
        // Show spinner while we send to backend... could potentially remove this but for now,
        // Preventing UI interaction while we sync with server
        // showProgressSpinner(true);

        Log.d ("butterfly", "Calling API:");

        //Recording recording = entry.toRecording();

        BackendService.getInstance()
                .getApi()
                .createRecording(entry.toRecording())
                .enqueue(new BackendApiCallback<Recording>() {
                    @Override
                    public Context getContext() {
                        Log.d(TAG, "Getting Callback Context...");
                        return getApplicationContext();
                    }

                    @Override
                    public void onSuccess(@NonNull Recording data) {
                        Log.d(TAG, "Recording created with UUID: " + data.getUuid());
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Recording created with UUID: " + data.getUuid());
                        }
                        entry.setRecording(data);
                        showProgressSpinner(false);
                        onEntryUpdated(entry);
                        // TODO store .wav and .txt on azure
                    }

                    @Override
                    public void onError(@Nullable String error) {
                        Log.e(TAG, "Error on creating recording: " + error);
                        showProgressSpinner(false);
                    }

                    @Override
                    public void onAuthenticationLost() {
                        onSignOut();
                    }
                });

        }

    @Override
    public void onEntryUpdated(Entry entry) {
        FeedManager.getInstance(this).updateEntry(entry);
        final Fragment fragment = getFragmentInView();
        if (fragment instanceof FeedFragment) {
            ((FeedFragment) fragment).notifyDataSetChanged();
        }
        Log.d(TAG, "Entry updated. ");

//        Log.d(TAG, "Entry updated with status: " + entry.getOxfordStatus());
//        if (entry.getOxfordStatus() != SUCCESSFUL) {
//            return; // Currently just going to wait until transcription is done before updating entry on server
//            // Best case, server would be running the speech API, not the client...
//        }
        String transcription = entry.getFullTranscription();
        if (transcription == null || transcription.isEmpty()) {
            Log.w(TAG, "Entry updated but no transcription found");
            return;
        }
        Recording recording = entry.getRecording();
        if (recording == null || recording.getUuid() == null) {
            Log.w(TAG, "Entry updated but no Recording was associated");
            return; // Currently no server recording associated with this entry
            // TODO should we create one in this case?
            // ^^ watch out for race condition if speech API comes back faster than our original create Recording call
        }
        recording.setTranscription(transcription);

        Log.d(TAG, "Updating entry with transcription:\n" + transcription);
        BackendService.getInstance()
                .getApi()
                .updateRecording(recording.getUuid(), recording)
                .enqueue(new BackendApiCallback<Recording>() {
                    @Override
                    public Context getContext() {
                        return getApplicationContext();
                    }

                    @Override
                    public void onSuccess(@NonNull Recording data) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Recording update with UUID: " + data.getUuid());
                        }
                        showProgressSpinner(false);
                        // TODO update .txt on azure?
                    }

                    @Override
                    public void onError(@Nullable String error) {
                        Log.e(TAG, "Error on updating recording: " + error);
                    }

                    @Override
                    public void onAuthenticationLost() {
                        onSignOut();
                    }
                });
    }

    @Override
    public void onRecordStart() {
        /* commented out while disabling always listening.
        // we don't want to listen in a service while the app is in the foreground.
        stopSphynxService();
        end disable */
        lockOrientation();

    }

    @Override
    public void onRecordStop() {
        /* commented out while disabling always listening.
        // we don't want to listen in a service while the app is in the foreground.
        startSphynxService();
        end disable */
        unlockOrientation();
        showRecordingFragment(false);
    }

    /* commented out to disable always listening and prevent butterfly from stealing the microphone from other apps.
        private void startSphynxService() {
            while (!isServiceRunning(this)) {
                Log.d ("butterfly", "Starting Sphynx Service..");
                startService(sphynxIntent = new Intent(getApplicationContext(), SphynxService.class));
                if (!isServiceRunning(this)) {
                    Log.d("butterfly", "Service wasn't running ... sleeping.");
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        private void stopSphynxService() {
            while (isServiceRunning(this)) {
                Log.d ("butterfly", "Stopping Sphynx Service...");
                if (sphynxIntent != null) {
                    stopService(sphynxIntent);
                } else { // Need to create an Intent...
                    stopService(new Intent(getApplicationContext(), SphynxService.class));
                }
                if (isServiceRunning(this)) {
                    Log.d("butterfly", "Service was running ... sleeping.");
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        end disable */

        public void replaceFragment(@NonNull Fragment fragment,
                                    @NonNull String tag,
                                    boolean addToStack) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            try {
                transaction.replace(R.id.fragment_frame_layout, fragment, tag);
                if (addToStack) {
                    transaction.addToBackStack(null);
                }
                transaction.commit();
            } catch (final IllegalStateException e) {
                // Thrown if the user exists the app during this operation
                Log.e(TAG, String.format("Failed to display Fragment %s:\n%s", tag, e.getMessage()));
            }
        }

        /**
         * @return The Fragment currently shown in the main area (if any)
         */
    @Nullable
    public Fragment getFragmentInView() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_frame_layout);
    }

    @Override
    public void showToolbar(boolean show) {
        if (toolbar == null) {
            return;
        }
        if (show) {
            toolbar.setVisibility(View.VISIBLE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            toolbar.setVisibility(View.GONE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    @Override
    public void setToolbarState(@NonNull ButterflyToolbar.State state) {
        if (toolbar != null) {
            toolbar.setState(state);
        }
    }

    @Override
    public void onSignOut() {
        BackendService.clearCredentials(getApplicationContext());
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void showEntryDetail(@NonNull Entry entry) {
        String tag = String.format(Locale.US, "Entry-%d", entry.getTimestamp());
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (!(fragment instanceof FeedDetailFragment)) {
            fragment = FeedDetailFragment.create(entry);
        }
        replaceFragment(fragment, tag, true);
    }

    private void onAboutClick() {
        String url = "https://windowmirror.org";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
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
            Log.i(TAG, "Sphynx Broadcast Received");
            toggleRecording();
        }
    }

    /* commented out while disabling always listening.
    // we don't want to listen in a service while the app is in the foreground.

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
    end disable */

    /**
     * Allows the user to rotate their screen
     */
    private void unlockOrientation() {
        setRequestedOrientation(prevOrientation);
    }

    /**
     * Prevents the user from rotating their screen and restarting the Activity.
     */
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
            default:
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
                    (entry.getOxfordStatus() == OxfordStatus.NONE && entry.getFullTranscription().isEmpty() ||
                            (entry.getOxfordStatus() == OxfordStatus.PENDING
                                    && System.currentTimeMillis() - entry.getTimestamp() > 300000))) {
                sendEntryToSpeechApi(entry);
                ++count;
            }
            if (count == MAX_ENTRY_RETRY_QUEUE) {
                return;
            }
        }
    }

    private void sendEntryToSpeechApi(final Entry entry) {
        Log.d(TAG, "Sending Entry to Speech API: " + entry.getTimestamp());
        final Intent intent = new Intent(this, SpeechApiService.class);
        intent.putExtra(SpeechApiService.KEY_ENTRY, entry);
        startService(intent);
    }
}
