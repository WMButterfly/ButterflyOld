package com.windowmirror.android.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import com.windowmirror.android.util.LocalPrefs;

/**
 * @author alliecurry
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BootReceiver", "--- onReceive");
        if (LocalPrefs.getIsBackgroundService(context)) {
            Log.d("BootReceiver", "--- Starting Sphynx Service...");
            context.startService(new Intent(context, SphynxService.class));
        }
    }

    /** Enabled the BootReceiver until explicitly disabled. */
    public static void enable(final Context context) {
        final ComponentName receiver = new ComponentName(context, BootReceiver.class);
        final PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
}
