package com.zjf.seeme.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.zjf.seeme.R;
import com.zjf.seeme.SeeMeApp;
import com.zjf.seeme.ui.MainActivity;

public class KeepAliveService extends Service {

    private static final String TAG = "KeepAliveService";
    private static final int NOTIF_ID = 8889;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground", e);
        }

        handler.postDelayed(() -> {
            tryReviveNotificationListener();
            ensureAlarmWatchdog();
            handler.postDelayed(this::stopSelf, 2000);
        }, 3000);

        return START_NOT_STICKY;
    }

    private void tryReviveNotificationListener() {
        if (NotificationListener.isRunning()) {
            Log.d(TAG, "NotificationListener already running");
            return;
        }

        Log.w(TAG, "Attempting to revive NotificationListener");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.service.notification.NotificationListenerService.requestRebind(
                        new ComponentName(this, NotificationListener.class));
                Log.d(TAG, "Rebind requested");
            }
        } catch (Exception e) {
            Log.e(TAG, "Rebind failed", e);
        }
    }

    private void ensureAlarmWatchdog() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Intent intent = new Intent(this, WatchdogReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, 7777, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 3 * 60_000,
                    3 * 60_000,
                    pi);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set alarm", e);
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, SeeMeApp.CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("SeeME")
                .setContentText("正在恢复服务...")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(false)
                .setContentIntent(pi)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIF_ID);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
