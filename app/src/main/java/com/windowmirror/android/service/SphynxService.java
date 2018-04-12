package com.windowmirror.android.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.windowmirror.android.controller.MainActivity;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

/**
 * Service Class which runs Pocket Sphynx.
 * @author alliecurry
 */
public class SphynxService extends Service implements RecognitionListener {
    private static final String TAG = SphynxService.class.getSimpleName();
    public static final String ACTION_START = "wmstart";
    public static final String ACTION_STOP = "wmstop";
    private static final String START_PHRASE = "hay butterfly";
    private static final String STOP_PHRASE = "thanks butterfly";
    public static final String KEY_START = "wm1";
    public static final String KEY_STOP = "wm2";
    private SpeechRecognizer recognizer;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            try {
                final Assets assets = new Assets(getApplicationContext());
                final File assetDir = assets.syncAssets();
                setupRecognizer(assetDir);
                startRecognizer(KEY_START);
            } catch (final IOException | RuntimeException e) {
                Log.e(TAG, "Could not start Sphynx: " + e.toString());
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
//            stopSelf(msg.arg1);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand " + startId + ": " + intent);

        Message msg = mServiceHandler.obtainMessage();
//        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_STICKY;
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict")) //TODO: Figure out why this is 'incomplete'
                // To disable logging of raw audio comment out this call (takes a lot of space on the device)
//                .setRawLogDir(assetsDir)
                // Threshold to tune for keyphrase to balance between false alarms and misses
                // values range from -beam (1e-50) to 1.0
                .setKeywordThreshold(1e-10f)
                // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)
                .getRecognizer();
        recognizer.addListener(this);
        recognizer.addKeyphraseSearch(KEY_START, START_PHRASE);
        recognizer.addKeyphraseSearch(KEY_STOP, STOP_PHRASE);
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {

    }

    private static final Pattern startPhrasePattern = Pattern.compile("(?i)h(a)?(e)?y (?i)butterfly");

    @Override
    public void onPartialResult(Hypothesis hypothesis) {

        if (hypothesis == null) {
            return;
        }
        final String text = hypothesis.getHypstr();

        //TODO:  Use Soundex??
        Matcher m = startPhrasePattern.matcher(text);
        if (m.find()) {
            openMainApp(true);
            stopSelf();
        }
        /*
        if (text.equalsIgnoreCase(START_PHRASE)) {
            openMainApp(true);
            stopSelf();
//            startRecognizer(KEY_STOP);
        } else if (text.equalsIgnoreCase(STOP_PHRASE)) {
            openMainApp(false);
            stopSelf();
//            startRecognizer(KEY_START);
        }
        */
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            Log.d(TAG, ">>> Final Result: " + hypothesis.getHypstr());
        }
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Recognizer Returned Error:\n" + e);
    }

    @Override
    public void onTimeout() {
        startRecognizer(KEY_START);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    private void startRecognizer(final String key) throws RuntimeException {
        if (recognizer == null) {
            Log.w(TAG, "Could not start Sphynx Recognizer: No recognizer instance found.");
            return;
        }

        recognizer.stop();
        recognizer.startListening(key);
        Log.d(TAG, "Recognizer started");
    }

    private void openMainApp(final boolean isStart) {
        // First broadcast message, then open app if needed
        final Intent localIntent =  new Intent(isStart ? ACTION_START : ACTION_STOP);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        final Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        intent.setComponent(new ComponentName(getApplicationContext().getPackageName(),
                MainActivity.class.getName()));
        intent.putExtra(KEY_START, isStart);
        startActivity(intent);
    }
}
