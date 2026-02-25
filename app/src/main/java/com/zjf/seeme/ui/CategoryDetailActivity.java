package com.zjf.seeme.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.zjf.seeme.R;
import com.zjf.seeme.data.AppDatabase;
import com.zjf.seeme.data.entity.MessageEntity;
import com.zjf.seeme.data.entity.TodoEntity;
import com.zjf.seeme.service.NotificationListener;
import com.zjf.seeme.ui.adapter.MessageAdapter;
import com.zjf.seeme.util.ThemeHelper;

import java.util.concurrent.Executors;

public class CategoryDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private AppDatabase db;
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        categoryName = getIntent().getStringExtra("category_name");
        if (categoryName == null) {
            finish();
            return;
        }

        db = AppDatabase.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(categoryName);
        toolbar.setNavigationOnClickListener(v -> finish());

        SwipeRefreshLayout swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerMessages);

        adapter = new MessageAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnMessageClickListener(new MessageAdapter.OnMessageClickListener() {
            @Override
            public void onMessageClick(MessageEntity message) {
                if (!message.isRead()) {
                    Executors.newSingleThreadExecutor().execute(() ->
                            db.messageDao().markAsRead(message.getId()));
                }
            }

            @Override
            public void onMessageLongClick(MessageEntity message) {
                showMessageOptions(message);
            }

            @Override
            public void onMessageDelete(MessageEntity message) {
                new MaterialAlertDialogBuilder(CategoryDetailActivity.this)
                        .setTitle("删除消息")
                        .setMessage("确定要删除这条消息吗？")
                        .setPositiveButton("删除", (d, w) ->
                                Executors.newSingleThreadExecutor().execute(() ->
                                        db.messageDao().delete(message)))
                        .setNegativeButton("取消", null)
                        .show();
            }

            @Override
            public void onOpenApp(MessageEntity message) {
                launchOriginalApp(message);
            }
        });

        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.accent, null));
        swipeRefresh.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.surface, null));
        swipeRefresh.setOnRefreshListener(() -> swipeRefresh.setRefreshing(false));

        db.messageDao().getByCategory(categoryName).observe(this, messages -> {
            if (messages != null) {
                adapter.submitList(messages);
                toolbar.setSubtitle(messages.size() + " 条消息");
            }
        });
    }

    private void launchOriginalApp(MessageEntity message) {
        PendingIntent cached = NotificationListener.getCachedIntent(message.getId());
        if (cached != null) {
            try {
                cached.send();
            } catch (PendingIntent.CanceledException e) {
                Log.d("CategoryDetail", "Cached PendingIntent expired, falling back");
                launchAppByPackage(message);
            }
        } else {
            launchAppByPackage(message);
        }
    }

    private void launchAppByPackage(MessageEntity message) {
        try {
            String packageName = message.getPackageName();
            if (packageName == null || packageName.isEmpty()) return;

            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "无法打开应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMessageOptions(MessageEntity message) {
        String[] options = {"打开原始应用", "标记已读", "转为待办", "删除消息"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(message.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            launchOriginalApp(message);
                            break;
                        case 1:
                            Executors.newSingleThreadExecutor().execute(() ->
                                    db.messageDao().markAsRead(message.getId()));
                            break;
                        case 2:
                            Executors.newSingleThreadExecutor().execute(() -> convertToTodo(message));
                            break;
                        case 3:
                            Executors.newSingleThreadExecutor().execute(() ->
                                    db.messageDao().delete(message));
                            break;
                    }
                })
                .show();
    }

    private void convertToTodo(MessageEntity message) {
        TodoEntity todo = new TodoEntity();
        todo.setMessageId(message.getId());
        todo.setTitle(message.getTitle());
        todo.setContent(message.getContent());
        todo.setFromApp(message.getAppName());
        todo.setCreatedAt(System.currentTimeMillis());
        todo.setDone(false);
        todo.setPriority(Math.min(5, Math.max(1, message.getImportance() / 2)));

        db.todoDao().insert(todo);

        runOnUiThread(() ->
                Snackbar.make(recyclerView, "已添加到待办", Snackbar.LENGTH_SHORT).show());
    }
}
