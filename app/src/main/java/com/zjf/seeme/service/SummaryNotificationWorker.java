package com.zjf.seeme.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.zjf.seeme.R;
import com.zjf.seeme.SeeMeApp;
import com.zjf.seeme.data.AppDatabase;
import com.zjf.seeme.data.entity.MessageEntity;
import com.zjf.seeme.data.entity.TodoEntity;
import com.zjf.seeme.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class SummaryNotificationWorker extends Worker {

    private static final int NOTIF_ID_IMPORTANT = 9001;
    private static final int NOTIF_ID_TODO = 9002;
    private static final int NOTIF_ID_GENERAL = 9003;
    private static final int MAX_DISPLAY = 3;

    public SummaryNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return Result.success();

        nm.cancel(NOTIF_ID_IMPORTANT);
        nm.cancel(NOTIF_ID_TODO);
        nm.cancel(NOTIF_ID_GENERAL);

        AppDatabase db = AppDatabase.getInstance(context);
        List<MessageEntity> unread = db.messageDao().getUnreadSync();

        List<MessageEntity> important = new ArrayList<>();
        List<MessageEntity> general = new ArrayList<>();

        for (MessageEntity msg : unread) {
            if (msg.getImportance() >= 7) {
                important.add(msg);
            } else {
                general.add(msg);
            }
        }

        postImportantNotification(context, nm, important);
        postTodoNotification(context, nm, db);
        postGeneralNotification(context, nm, general);

        updateMonitoringNotification(context, nm, db);

        return Result.success();
    }

    private void postImportantNotification(Context context, NotificationManager nm,
                                            List<MessageEntity> messages) {
        if (messages.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        int count = Math.min(messages.size(), MAX_DISPLAY);
        for (int i = 0; i < count; i++) {
            MessageEntity msg = messages.get(i);
            sb.append("🔴 [").append(msg.getAppName()).append("] ").append(msg.getTitle());
            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                String preview = msg.getContent().length() > 30
                        ? msg.getContent().substring(0, 30) + "..."
                        : msg.getContent();
                sb.append(": ").append(preview);
            }
            if (i < count - 1) sb.append("\n");
        }
        if (messages.size() > MAX_DISPLAY) {
            sb.append("\n还有 ").append(messages.size() - MAX_DISPLAY).append(" 条重要消息...");
        }

        PendingIntent pi = createMainIntent(context, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SeeMeApp.CHANNEL_SUMMARY)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("🔴 重要消息 · " + messages.size() + "条")
                .setContentText(messages.get(0).getTitle())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(sb.toString()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSortKey("A");

        nm.notify(NOTIF_ID_IMPORTANT, builder.build());
    }

    private void postTodoNotification(Context context, NotificationManager nm, AppDatabase db) {
        List<TodoEntity> todos = db.todoDao().getActiveTodosSync();
        if (todos.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        int count = Math.min(todos.size(), MAX_DISPLAY);
        for (int i = 0; i < count; i++) {
            TodoEntity todo = todos.get(i);
            sb.append("📌 ").append(todo.getTitle());
            if (todo.getFromApp() != null && !todo.getFromApp().isEmpty()) {
                sb.append(" (").append(todo.getFromApp()).append(")");
            }
            if (i < count - 1) sb.append("\n");
        }
        if (todos.size() > MAX_DISPLAY) {
            sb.append("\n还有 ").append(todos.size() - MAX_DISPLAY).append(" 项待办...");
        }

        PendingIntent pi = createMainIntent(context, 2);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SeeMeApp.CHANNEL_SUMMARY)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("📌 待办事项 · " + todos.size() + "项")
                .setContentText(todos.get(0).getTitle())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(sb.toString()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSortKey("B");

        nm.notify(NOTIF_ID_TODO, builder.build());
    }

    private void postGeneralNotification(Context context, NotificationManager nm,
                                          List<MessageEntity> messages) {
        if (messages.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        int count = Math.min(messages.size(), MAX_DISPLAY);
        for (int i = 0; i < count; i++) {
            MessageEntity msg = messages.get(i);
            sb.append("⚪ [").append(msg.getAppName()).append("] ").append(msg.getTitle());
            if (i < count - 1) sb.append("\n");
        }
        if (messages.size() > MAX_DISPLAY) {
            sb.append("\n还有 ").append(messages.size() - MAX_DISPLAY).append(" 条消息...");
        }

        PendingIntent pi = createMainIntent(context, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SeeMeApp.CHANNEL_SUMMARY)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("⚪ 一般消息 · " + messages.size() + "条")
                .setContentText(messages.get(0).getTitle())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(sb.toString()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pi)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSortKey("C");

        nm.notify(NOTIF_ID_GENERAL, builder.build());
    }

    private void updateMonitoringNotification(Context context, NotificationManager nm, AppDatabase db) {
        int totalMessages = db.messageDao().getTotalCountSync();
        int categoryCount = db.messageDao().getDistinctCategoryCountSync();
        int todoCount = db.todoDao().getActiveCountSync();

        String statusText = String.format("综合 %d 条 · %d 个分类 · 待办 %d 项",
                totalMessages, categoryCount, todoCount);

        PendingIntent pi = createMainIntent(context, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SeeMeApp.CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("SeeME 运行中")
                .setContentText(statusText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pi);

        nm.notify(8888, builder.build());
    }

    private PendingIntent createMainIntent(Context context, int tabIndex) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("tab_index", tabIndex);
        return PendingIntent.getActivity(
                context, tabIndex, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
