package com.windowmirror.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import java.io.File;
import java.io.IOException;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

/**
 * Service Class which runs Pocket Sphynx.
 * @author alliecurry
 */
public class SphynxService extends Service implements RecognitionListener {
    private static final String TAG = SphynxService.class.getSimpleName();
    private static final String START_KEYWORD = "okay window mirror";
    private static final String STOP_KEYWORD = "thank you window mirror";
    private static final String KEY_START = "wm1";
    private static final String KEY_STOP = "wm2";
    private SpeechRecognizer recognizer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("SphynxService", "onStartCommand " + startId + ": " + intent);

        try {
            final Assets assets = new Assets(getApplicationContext());
            final File assetDir = assets.syncAssets();
            setupRecognizer(assetDir);
            startRecognizer(START_KEYWORD);
        } catch (final IOException | RuntimeException e) {
            Log.e(TAG, "Could not start Sphynx: " + e.toString());
        }

        return START_STICKY;
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                        // To disable logging of raw audio comment out this call (takes a lot of space on the device)
//                .setRawLogDir(assetsDir)
                        // Threshold to tune for keyphrase to balance between false alarms and misses
                .setKeywordThreshold(1e-15f)
                        // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)
                .getRecognizer();
        recognizer.addListener(this);
        recognizer.addKeyphraseSearch(KEY_START, START_KEYWORD);
        recognizer.addKeyphraseSearch(KEY_STOP, STOP_KEYWORD);
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return;
        }
        final String text = hypothesis.getHypstr();
        Log.v(TAG, "---> " + text + " / " + hypothesis.getProb() + " / " + hypothesis.getBestScore());
        if (text.equalsIgnoreCase(START_KEYWORD)) {
            // TODO open app / start recording
            startRecognizer(STOP_KEYWORD);
        } else if (text.equalsIgnoreCase(STOP_KEYWORD)) {
            // TODO stop recording
            startRecognizer(START_KEYWORD);
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            Toast.makeText(getApplicationContext(),
                    hypothesis.getHypstr(),
                    Toast.LENGTH_SHORT)
                    .show();
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
}
