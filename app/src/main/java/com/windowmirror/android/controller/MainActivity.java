package com.windowmirror.android.controller;

import android.app.Activity;
import android.os.Bundle;
import com.windowmirror.android.R;

/**
 * The Activity that starts on app launch.
 * @author alliecurry
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
