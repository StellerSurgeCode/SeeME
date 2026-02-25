package com.zjf.seeme;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;

public class SeeMeApp extends Application {

    public static final String CHANNEL_SUMMARY = "seeme_summary";
    public static final String CHANNEL_SERVICE = "seeme_service";
    public static final String CHANNEL_IMPORTANT = "seeme_important";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        NotificationChannel summaryChannel = new NotificationChannel(
                CHANNEL_SUMMARY,
                "消息汇总",
                NotificationManager.IMPORTANCE_HIGH
        );
        summaryChannel.setDescription("重要消息、待办事项和一般消息的汇总通知");

        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_SERVICE,
                "监控服务",
                NotificationManager.IMPORTANCE_LOW
        );
        serviceChannel.setDescription("SeeME 通知监听服务运行状态");
        serviceChannel.setShowBadge(false);

        NotificationChannel importantChannel = new NotificationChannel(
                CHANNEL_IMPORTANT,
                "重要消息即时通知",
                NotificationManager.IMPORTANCE_HIGH
        );
        importantChannel.setDescription("重要性≥5的消息将立即发出通知和声音");
        importantChannel.enableVibration(true);
        importantChannel.setVibrationPattern(new long[]{0, 300, 200, 300});
        importantChannel.setBypassDnd(true);
        AudioAttributes audioAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        importantChannel.setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                audioAttrs
        );
        importantChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(summaryChannel);
            manager.createNotificationChannel(serviceChannel);
            manager.createNotificationChannel(importantChannel);
        }
    }
}
