package com.zjf.seeme.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {

            Log.d(TAG, "Boot/update received: " + action);

            WorkScheduler.scheduleSummaryWork(context);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    android.service.notification.NotificationListenerService.requestRebind(
                            new ComponentName(context, NotificationListener.class));
                    Log.d(TAG, "Requested NotificationListener rebind");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to rebind NotificationListener", e);
            }
        }
    }
}
