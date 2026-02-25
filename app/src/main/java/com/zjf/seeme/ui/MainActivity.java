package com.zjf.seeme.ui;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.zjf.seeme.R;
import com.zjf.seeme.data.AppDatabase;
import com.zjf.seeme.data.PrefsManager;
import com.zjf.seeme.data.entity.CategoryEntity;
import com.zjf.seeme.SeeMeApp;
import com.zjf.seeme.service.NotificationListener;
import com.zjf.seeme.service.WorkScheduler;
import com.zjf.seeme.ui.adapter.ViewPagerAdapter;
import com.zjf.seeme.util.ThemeHelper;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TextView tvUnreadBadge;
    private EditText etSearch;
    private PrefsManager prefs;
    private AppDatabase db;

    private final String[] tabTitles = {"综合", "分类", "待办"};

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    showMonitoringNotification();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PrefsManager.getInstance(this);
        db = AppDatabase.getInstance(this);

        initViews();
        initDefaultCategories();
        checkNotificationAccess();
        checkBatteryOptimization();
        requestNotificationPermission();

        WorkScheduler.scheduleSummaryWork(this);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int removed = db.messageDao().deleteDuplicates();
                if (removed > 0) {
                    Log.d("MainActivity", "Auto-cleaned " + removed + " duplicate messages");
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error auto-cleaning duplicates", e);
            }
        });

        handleIntentTab(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureListenerAlive();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntentTab(intent);
    }

    private void ensureListenerAlive() {
        if (!isNotificationListenerEnabled()) return;

        if (!NotificationListener.isRunning()) {
            Log.w("MainActivity", "NotificationListener not running, requesting rebind");
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    android.service.notification.NotificationListenerService.requestRebind(
                            new ComponentName(this, NotificationListener.class));
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Failed to rebind", e);
            }
        }
    }

    private void handleIntentTab(Intent intent) {
        if (intent != null && intent.hasExtra("tab_index")) {
            int tabIndex = intent.getIntExtra("tab_index", 0);
            if (viewPager != null && tabIndex >= 0 && tabIndex < tabTitles.length) {
                viewPager.setCurrentItem(tabIndex, true);
            }
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        tvUnreadBadge = findViewById(R.id.tvUnreadBadge);
        etSearch = findViewById(R.id.etSearch);
        ImageView btnSettings = findViewById(R.id.btnSettings);
        ImageView btnClearAll = findViewById(R.id.btnClearAll);

        ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        btnClearAll.setOnClickListener(v -> showClearAllDialog());

        db.messageDao().getTotalUnreadCount().observe(this, count -> {
            if (count != null && count > 0) {
                tvUnreadBadge.setVisibility(View.VISIBLE);
                tvUnreadBadge.setText(count + " 条未读");
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                return true;
            }
            return false;
        });
    }

    public String getCurrentSearchQuery() {
        return etSearch != null ? etSearch.getText().toString().trim() : "";
    }

    private void initDefaultCategories() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (db.categoryDao().getAllSync().isEmpty()) {
                db.categoryDao().insert(new CategoryEntity("工作", "ic_work", 0xFF4A90D9, true, 1));
                db.categoryDao().insert(new CategoryEntity("生活", "ic_life", 0xFF50C878, true, 2));
                db.categoryDao().insert(new CategoryEntity("娱乐", "ic_entertainment", 0xFFE8A838, true, 3));
                db.categoryDao().insert(new CategoryEntity("广告推销", "ic_ads", 0xFFD94A4A, true, 4));
                db.categoryDao().insert(new CategoryEntity("综合", "ic_general", 0xFF7B68AE, true, 5));
            }
        });
    }

    private void checkNotificationAccess() {
        if (!isNotificationListenerEnabled()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("需要通知监听权限")
                    .setMessage("SeeME 需要通知监听权限来读取和整理您的通知消息。请在设置中开启。")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    })
                    .setNegativeButton("稍后", null)
                    .setCancelable(false)
                    .show();
        }
    }

    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("后台运行权限")
                    .setMessage("为确保消息监听服务持续运行，请允许 SeeME 在后台不受限制地运行。")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("稍后", null)
                    .show();
        }
    }

    private boolean isNotificationListenerEnabled() {
        ComponentName cn = new ComponentName(this, NotificationListener.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        showMonitoringNotification();
    }

    private void showClearAllDialog() {
        String[] options = {"清除重复消息", "删除所有消息和待办"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("消息管理")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            int removed = db.messageDao().deleteDuplicates();
                            runOnUiThread(() -> {
                                Toast.makeText(this,
                                        "已清除 " + removed + " 条重复消息",
                                        Toast.LENGTH_SHORT).show();
                                refreshMonitoringNotification();
                            });
                        });
                    } else {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("确认删除")
                                .setMessage("确定要删除所有消息和待办事项吗？此操作不可撤销。\n（监控服务通知不受影响）")
                                .setPositiveButton("全部删除", (d2, w2) -> {
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        db.messageDao().deleteAll();
                                        db.todoDao().deleteAll();
                                        runOnUiThread(() -> {
                                            Toast.makeText(this, "已清除所有消息和待办", Toast.LENGTH_SHORT).show();
                                            refreshMonitoringNotification();
                                        });
                                    });
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void refreshMonitoringNotification() {
        Executors.newSingleThreadExecutor().execute(() -> {
            int totalMessages = db.messageDao().getTotalCountSync();
            int categoryCount = db.messageDao().getDistinctCategoryCountSync();
            int todoCount = db.todoDao().getActiveCountSync();

            String statusText = String.format("综合 %d 条 · %d 个分类 · 待办 %d 项",
                    totalMessages, categoryCount, todoCount);

            runOnUiThread(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SeeMeApp.CHANNEL_SERVICE)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("SeeME 运行中")
                        .setContentText(statusText)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true)
                        .setContentIntent(pi);

                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.notify(8888, builder.build());
                }
            });
        });
    }

    private void showMonitoringNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            int totalMessages = db.messageDao().getTotalCountSync();
            int categoryCount = db.messageDao().getDistinctCategoryCountSync();
            int todoCount = db.todoDao().getActiveCountSync();

            String statusText = String.format("综合 %d 条 · %d 个分类 · 待办 %d 项",
                    totalMessages, categoryCount, todoCount);

            runOnUiThread(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SeeMeApp.CHANNEL_SERVICE)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("SeeME 运行中")
                        .setContentText(statusText)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true)
                        .setContentIntent(pi);

                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.notify(8888, builder.build());
                }
            });
        });
    }
}
