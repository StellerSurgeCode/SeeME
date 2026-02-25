package com.zjf.seeme.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class WatchdogReceiver extends BroadcastReceiver {

    private static final String TAG = "WatchdogReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Watchdog alarm fired");

        if (NotificationListener.isRunning()) {
            Log.d(TAG, "NotificationListener is alive");
            return;
        }

        Log.w(TAG, "NotificationListener is dead, attempting recovery");

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.service.notification.NotificationListenerService.requestRebind(
                        new ComponentName(context, NotificationListener.class));
                Log.d(TAG, "Rebind requested via watchdog");
            }
        } catch (Exception e) {
            Log.e(TAG, "Rebind failed", e);
        }

        try {
            Intent keepAlive = new Intent(context, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(keepAlive);
            } else {
                context.startService(keepAlive);
            }
        } catch (Exception e) {
            Log.e(TAG, "KeepAlive launch failed", e);
        }
    }
}
