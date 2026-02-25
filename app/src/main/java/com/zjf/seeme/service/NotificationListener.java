package com.zjf.seeme.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.util.LruCache;

import androidx.core.app.NotificationCompat;

import com.zjf.seeme.R;
import com.zjf.seeme.SeeMeApp;
import com.zjf.seeme.data.AppDatabase;
import com.zjf.seeme.data.PrefsManager;
import com.zjf.seeme.data.dao.WhitelistDao;
import com.zjf.seeme.data.entity.KeywordRule;
import com.zjf.seeme.data.entity.MessageEntity;
import com.zjf.seeme.data.entity.TodoEntity;
import com.zjf.seeme.ui.MainActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";
    private static final int NOTIF_ID_MONITORING = 8888;
    private static final long DEDUP_WINDOW_MS = 30_000;
    private static final long HEARTBEAT_INTERVAL_MS = 2 * 60_000;
    private static final long RECONNECT_QUIET_PERIOD_MS = 5_000;

    private String selfPackage;
    private AppDatabase db;
    private PrefsManager prefs;
    private ExecutorService executor;
    private PowerManager.WakeLock wakeLock;
    private Handler mainHandler;
    private volatile boolean isConnected = false;

    private volatile long lastConnectedTimestamp = 0;
    private final AtomicBoolean inQuietPeriod = new AtomicBoolean(false);

    private final Set<String> pendingCancelKeys = ConcurrentHashMap.newKeySet();

    private static volatile NotificationListener instance;

    private static final LruCache<Long, PendingIntent> pendingIntentCache = new LruCache<>(200);
    private final ConcurrentHashMap<String, Long> recentNotifications = new ConcurrentHashMap<>();

    public static NotificationListener getInstance() {
        return instance;
    }

    public static boolean isRunning() {
        return instance != null && instance.isConnected;
    }

    public static PendingIntent getCachedIntent(long messageId) {
        return pendingIntentCache.get(messageId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        selfPackage = getPackageName();
        db = AppDatabase.getInstance(this);
        prefs = PrefsManager.getInstance(this);
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "NotificationListener created");

        acquireWakeLock();
        promoteToForeground();
        startHeartbeat();
        scheduleAlarmWatchdog();
    }

    // ==================== WakeLock ====================

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                if (wakeLock != null && wakeLock.isHeld()) return;
                wakeLock = pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "SeeME:NotifListenerLock");
                wakeLock.acquire(24 * 60 * 60 * 1000L);
                Log.d(TAG, "WakeLock acquired");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire WakeLock", e);
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {}
    }

    // ==================== Foreground Service ====================

    private void promoteToForeground() {
        try {
            Notification notification = buildMonitoringNotification(0, 0, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID_MONITORING, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID_MONITORING, notification);
            }
            Log.d(TAG, "Promoted to foreground");
        } catch (Exception e) {
            Log.e(TAG, "Failed to startForeground", e);
            updateMonitoringNotification(0, 0, 0);
        }
    }

    private Notification buildMonitoringNotification(int totalMessages, int categoryCount, int todoCount) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long uptimeMin = (SystemClock.elapsedRealtime() - (lastConnectedTimestamp > 0
                ? lastConnectedTimestamp : SystemClock.elapsedRealtime())) / 60_000;
        String uptimeStr = uptimeMin < 1 ? "刚刚启动" : "已运行 " + uptimeMin + " 分钟";

        String statusText = String.format("综合 %d 条 · %d 分类 · 待办 %d · %s",
                totalMessages, categoryCount, todoCount, uptimeStr);

        return new NotificationCompat.Builder(this, SeeMeApp.CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("SeeME 运行中")
                .setContentText(statusText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentIntent(pi)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    private void updateMonitoringNotification(int totalMessages, int categoryCount, int todoCount) {
        try {
            Notification notification = buildMonitoringNotification(totalMessages, categoryCount, todoCount);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIF_ID_MONITORING, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating monitoring notification", e);
        }
    }

    private void refreshMonitoringNotification() {
        executor.execute(() -> {
            try {
                int total = db.messageDao().getTotalCountSync();
                int cats = db.messageDao().getDistinctCategoryCountSync();
                int todos = db.todoDao().getActiveCountSync();
                updateMonitoringNotification(total, cats, todos);
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing notification", e);
            }
        });
    }

    // ==================== Heartbeat + AlarmManager Watchdog ====================

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Heartbeat tick, connected=" + isConnected);

            if (!isConnected) {
                Log.w(TAG, "Heartbeat: disconnected, rebinding");
                tryRequestRebind();
            }

            if (wakeLock == null || !wakeLock.isHeld()) {
                acquireWakeLock();
            }

            refreshMonitoringNotification();
            retryPendingCancels();

            mainHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
        }
    };

    private void startHeartbeat() {
        mainHandler.removeCallbacks(heartbeatRunnable);
        mainHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
    }

    private void stopHeartbeat() {
        mainHandler.removeCallbacks(heartbeatRunnable);
    }

    private void scheduleAlarmWatchdog() {
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
            Log.d(TAG, "AlarmManager watchdog scheduled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm watchdog", e);
        }
    }

    private void cancelAlarmWatchdog() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent intent = new Intent(this, WatchdogReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, 7777, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi);
        } catch (Exception ignored) {}
    }

    // ==================== Connection Management ====================

    private void tryRequestRebind() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requestRebind(new ComponentName(this, NotificationListener.class));
                Log.d(TAG, "Rebind requested");
            }
        } catch (Exception e) {
            Log.e(TAG, "Rebind failed", e);
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        isConnected = true;
        lastConnectedTimestamp = SystemClock.elapsedRealtime();
        Log.d(TAG, "Listener connected");

        inQuietPeriod.set(true);
        mainHandler.postDelayed(() -> {
            inQuietPeriod.set(false);
            Log.d(TAG, "Quiet period ended, starting normal operation");
            clearAllExceptWhitelisted();
        }, RECONNECT_QUIET_PERIOD_MS);

        refreshMonitoringNotification();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        isConnected = false;
        Log.w(TAG, "Listener disconnected");
        tryRequestRebind();
    }

    // ==================== Notification Cancel with Retry ====================

    private void tryCancelNotification(String key) {
        try {
            cancelNotification(key);
            pendingCancelKeys.remove(key);
        } catch (Exception e) {
            Log.w(TAG, "Cancel failed for " + key + ", queuing retry");
            pendingCancelKeys.add(key);
        }
    }

    private void retryPendingCancels() {
        if (pendingCancelKeys.isEmpty() || !isConnected) return;

        Set<String> toRetry = new HashSet<>(pendingCancelKeys);
        for (String key : toRetry) {
            try {
                cancelNotification(key);
                pendingCancelKeys.remove(key);
            } catch (Exception e) {
                Log.w(TAG, "Retry cancel still failed: " + key);
            }
        }
    }

    public void clearAllExceptWhitelisted() {
        executor.execute(() -> {
            try {
                if (!isConnected) return;
                StatusBarNotification[] active = getActiveNotifications();
                if (active == null) return;

                String myPkg = getPackageName();
                WhitelistDao whitelistDao = db.whitelistDao();
                for (StatusBarNotification sbn : active) {
                    String pkg = sbn.getPackageName();
                    if (myPkg.equals(pkg)) continue;

                    boolean isOngoing = (sbn.getNotification().flags & Notification.FLAG_ONGOING_EVENT) != 0;
                    boolean isWhitelisted = whitelistDao.isWhitelisted(pkg);
                    if (isOngoing && isWhitelisted) continue;

                    tryCancelNotification(sbn.getKey());
                }
                Log.d(TAG, "Cleared notifications, pending retries: " + pendingCancelKeys.size());
            } catch (Exception e) {
                Log.e(TAG, "Error clearing notifications", e);
            }
        });
    }

    // ==================== Notification Processing ====================

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        String packageName = sbn.getPackageName();
        if (selfPackage != null && selfPackage.equals(packageName)) return;
        if (packageName != null && packageName.equals(getPackageName())) return;

        if (inQuietPeriod.get()) {
            Notification n = sbn.getNotification();
            boolean isOngoing = n != null && (n.flags & Notification.FLAG_ONGOING_EVENT) != 0;
            if (!isOngoing) {
                tryCancelNotification(sbn.getKey());
            }
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        boolean isOngoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        final PendingIntent contentIntent = notification.contentIntent;
        final String notifKey = sbn.getKey();

        if (!isOngoing) {
            tryCancelNotification(notifKey);
        }

        executor.execute(() -> {
            try {
                Bundle extras = notification.extras;
                String title = extras != null ? extras.getString(Notification.EXTRA_TITLE, "") : "";
                CharSequence textCs = extras != null ? extras.getCharSequence(Notification.EXTRA_TEXT) : null;
                String content = textCs != null ? textCs.toString() : "";
                CharSequence subTextCs = extras != null ? extras.getCharSequence(Notification.EXTRA_SUB_TEXT) : null;
                String sender = subTextCs != null ? subTextCs.toString() : "";

                if (sender.isEmpty() && extras != null) {
                    CharSequence convTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
                    if (convTitle != null) sender = convTitle.toString();
                }

                if (title.isEmpty() && content.isEmpty()) return;
                if (shouldSkipVagueNotification(title, content)) return;

                String dedupKey = packageName + "|" + title + "|" + content;
                long now = System.currentTimeMillis();
                Long lastSeen = recentNotifications.get(dedupKey);
                if (lastSeen != null && (now - lastSeen) < DEDUP_WINDOW_MS) {
                    return;
                }
                recentNotifications.put(dedupKey, now);
                cleanupOldDedupEntries(now);

                if (isOngoing && isDuplicateInDb(packageName, title, content)) {
                    return;
                }

                String appName = getAppName(packageName);
                long timestamp = sbn.getPostTime();

                String intentUri = null;
                if (contentIntent != null) {
                    intentUri = contentIntent.getCreatorPackage() + ":" + notifKey;
                }

                MessageEntity message = new MessageEntity();
                message.setPackageName(packageName);
                message.setAppName(appName);
                message.setTitle(title);
                message.setContent(content);
                message.setSenderName(sender);
                message.setTimestamp(timestamp);
                message.setRead(false);
                message.setCategory("综合");
                message.setImportance(5);
                message.setIntentUri(intentUri);
                message.setNotificationKey(notifKey);

                long msgId = db.messageDao().insert(message);

                if (contentIntent != null) {
                    pendingIntentCache.put(msgId, contentIntent);
                }

                boolean isBlacklisted = db.blacklistDao().isBlacklisted(packageName);
                int finalImportance = 5;
                if (!isBlacklisted) {
                    List<KeywordRule> rules = db.keywordRuleDao().getEnabledSync();
                    List<String> recentContext = buildRecentContext(packageName, sender);

                    DeepSeekService aiService = new DeepSeekService(prefs);
                    aiService.setKeywordRules(rules);
                    DeepSeekService.ClassificationResult result =
                            aiService.classifyMessage(appName, title, content, sender, timestamp, recentContext);

                    db.messageDao().updateClassification(msgId, result.category, result.importance);
                    ensureCategoryExists(result.category);
                    finalImportance = result.importance;

                    if (result.isTodo) {
                        createAiTodo(msgId, appName, sender, title, content, result.importance);
                    } else {
                        checkAndCreateTodo(msgId, appName, sender, title, content, result.importance);
                    }
                }

                message.setId(msgId);
                message.setImportance(finalImportance);
                sendImportantNotification(message, finalImportance);

                if (!isOngoing) {
                    tryCancelNotification(notifKey);
                }

                refreshMonitoringNotification();

            } catch (Exception e) {
                Log.e(TAG, "Error processing notification", e);
            }
        });
    }

    // ==================== Helpers ====================

    private boolean shouldSkipVagueNotification(String title, String content) {
        return (content == null || content.isEmpty()) && (title == null || title.isEmpty());
    }

    private boolean isDuplicateInDb(String packageName, String title, String content) {
        try {
            long cutoff = System.currentTimeMillis() - 300_000;
            return db.messageDao().countRecentDuplicate(packageName, title, content, cutoff) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void cleanupOldDedupEntries(long now) {
        recentNotifications.entrySet().removeIf(e -> (now - e.getValue()) > DEDUP_WINDOW_MS * 2);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null) {
            pendingCancelKeys.remove(sbn.getKey());
        }
    }

    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private void ensureCategoryExists(String categoryName) {
        if (db.categoryDao().getByName(categoryName) == null) {
            com.zjf.seeme.data.entity.CategoryEntity newCat =
                    new com.zjf.seeme.data.entity.CategoryEntity(
                            categoryName, "ic_category", 0xFF607D8B, false, 99);
            db.categoryDao().insert(newCat);
        }
    }

    // ==================== Important Notification ====================

    private static int importantNotifIdCounter = 9000;

    private void sendImportantNotification(MessageEntity message, int importance) {
        int threshold = prefs.getImportantThreshold();
        if (importance < threshold) return;

        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pi = PendingIntent.getActivity(this, importantNotifIdCounter, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String title = String.format("[重要 %d/10] %s", importance, message.getAppName());
            String sender = message.getSenderName();
            String body = "";
            if (sender != null && !sender.isEmpty()) {
                body = sender + ": ";
            }
            body += message.getContent() != null ? message.getContent() : message.getTitle();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SeeMeApp.CHANNEL_IMPORTANT)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(importantNotifIdCounter++, builder.build());
                if (importantNotifIdCounter > 9999) importantNotifIdCounter = 9000;
            }

            playAlertSound();
        } catch (Exception e) {
            Log.e(TAG, "Error sending important notification", e);
        }
    }

    private void playAlertSound() {
        try {
            int volume = prefs.getAlertVolume();
            if (volume <= 0) return;

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) return;

            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            int targetVol = (int) Math.ceil(maxVol * volume / 100.0);
            int originalVol = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetVol, 0);

            MediaPlayer mp = MediaPlayer.create(this, Settings.System.DEFAULT_NOTIFICATION_URI);
            if (mp != null) {
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                mp.setOnCompletionListener(player -> {
                    player.release();
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalVol, 0);
                });
                mp.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing alert sound", e);
        }
    }

    // ==================== Context & Todo ====================

    private List<String> buildRecentContext(String packageName, String sender) {
        List<String> contextLines = new ArrayList<>();
        try {
            long since = System.currentTimeMillis() - 3_600_000;
            List<MessageEntity> recentMsgs;
            if (sender != null && !sender.isEmpty()) {
                recentMsgs = db.messageDao().getRecentBySenderSync(packageName, sender, since, 10);
            } else {
                recentMsgs = db.messageDao().getRecentByPackageSync(packageName, since, 10);
            }
            if (recentMsgs != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                for (MessageEntity m : recentMsgs) {
                    StringBuilder line = new StringBuilder();
                    line.append("[").append(sdf.format(new Date(m.getTimestamp()))).append("] ");
                    if (m.getSenderName() != null && !m.getSenderName().isEmpty()) {
                        line.append(m.getSenderName()).append(": ");
                    }
                    if (m.getTitle() != null && !m.getTitle().isEmpty()) {
                        line.append(m.getTitle()).append(" - ");
                    }
                    line.append(m.getContent() != null ? m.getContent() : "");
                    contextLines.add(line.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error building recent context", e);
        }
        return contextLines;
    }

    private void createAiTodo(long msgId, String appName, String sender, String title,
                              String content, int importance) {
        String senderInfo = (sender != null && !sender.isEmpty()) ? sender : appName;
        TodoEntity todo = new TodoEntity();
        todo.setMessageId(msgId);
        todo.setTitle("AI识别待办 (" + senderInfo + ")");
        todo.setContent(content != null ? content : title);
        todo.setFromApp(appName);
        todo.setCreatedAt(System.currentTimeMillis());
        todo.setDone(false);
        todo.setPriority(Math.min(5, Math.max(1, importance / 2)));
        db.todoDao().insert(todo);
    }

    private void checkAndCreateTodo(long msgId, String appName, String sender, String title,
                                     String content, int importance) {
        String combined = ((title != null ? title : "") + " " + (content != null ? content : "")).toLowerCase();

        boolean isTodoCandidate = false;
        String todoTitle = null;

        if (combined.contains("开会") || combined.contains("会议") || combined.contains("开个会")) {
            isTodoCandidate = true;
            todoTitle = "参加会议";
        } else if (combined.contains("提醒") || combined.contains("别忘了") || combined.contains("记得")) {
            isTodoCandidate = true;
            todoTitle = "待处理提醒";
        } else if (combined.contains("截止") || combined.contains("deadline") || combined.contains("到期")) {
            isTodoCandidate = true;
            todoTitle = "截止日期提醒";
        } else if (combined.contains("等会") || combined.contains("等一下") || combined.contains("稍后")
                || combined.contains("待会") || combined.contains("一会儿")) {
            isTodoCandidate = true;
            todoTitle = "稍后处理";
        } else if (combined.contains("约") && (combined.contains("时间") || combined.contains("见面")
                || combined.contains("地点") || combined.contains("点"))) {
            isTodoCandidate = true;
            todoTitle = "约定事项";
        } else if (combined.contains("还款") || combined.contains("缴费") || combined.contains("付款")) {
            isTodoCandidate = true;
            todoTitle = "待付款/还款";
        } else if (combined.contains("面试") || combined.contains("考试") || combined.contains("报名")) {
            isTodoCandidate = true;
            todoTitle = "重要日程";
        }

        if (isTodoCandidate) {
            String senderInfo = (sender != null && !sender.isEmpty()) ? sender : appName;
            TodoEntity todo = new TodoEntity();
            todo.setMessageId(msgId);
            todo.setTitle(todoTitle + " (" + senderInfo + ")");
            todo.setContent(content != null ? content : title);
            todo.setFromApp(appName);
            todo.setCreatedAt(System.currentTimeMillis());
            todo.setDone(false);
            todo.setPriority(Math.min(5, Math.max(1, importance / 2)));
            db.todoDao().insert(todo);
        }
    }

    // ==================== Lifecycle ====================

    @Override
    public void onDestroy() {
        isConnected = false;
        instance = null;
        stopHeartbeat();
        releaseWakeLock();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIF_ID_MONITORING);
        }
        super.onDestroy();
        if (executor != null) executor.shutdown();

        Log.w(TAG, "Service destroyed, launching KeepAlive");
        try {
            Intent restartIntent = new Intent(this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start KeepAliveService", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
