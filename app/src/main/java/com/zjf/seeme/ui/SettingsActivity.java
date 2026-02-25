package com.zjf.seeme.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.zjf.seeme.R;
import com.zjf.seeme.data.AppDatabase;
import com.zjf.seeme.data.PrefsManager;
import com.zjf.seeme.data.entity.BlacklistApp;
import com.zjf.seeme.data.entity.WhitelistApp;
import com.zjf.seeme.service.NotificationListener;
import com.zjf.seeme.service.WorkScheduler;
import com.zjf.seeme.ui.adapter.BlacklistAdapter;
import com.zjf.seeme.ui.adapter.WhitelistAdapter;
import com.zjf.seeme.util.ThemeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private PrefsManager prefs;
    private AppDatabase db;
    private WhitelistAdapter whitelistAdapter;
    private BlacklistAdapter blacklistAdapter;

    private TextInputEditText etCustomPrompt;
    private TextInputEditText etDndStart, etDndEnd, etDndMinImportance;
    private TextInputEditText etImportantThreshold, etAlertVolume;
    private MaterialSwitch switchDnd, switchAdFilter;
    private TextView tvCurrentTheme;

    private static final String[][] THEME_COLORS = {
            {"#4A90D9", "经典蓝"},
            {"#50C878", "清新绿"},
            {"#7B68AE", "优雅紫"},
            {"#E8A838", "活力橙"},
            {"#E53935", "热情红"},
    };

    private View[] colorViews;
    private String selectedThemeColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = PrefsManager.getInstance(this);
        db = AppDatabase.getInstance(this);

        initToolbar();
        initViews();
        initThemeColors();
        loadSettings();
        setupWhitelist();
        setupBlacklist();
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etCustomPrompt = findViewById(R.id.etCustomPrompt);
        etDndStart = findViewById(R.id.etDndStart);
        etDndEnd = findViewById(R.id.etDndEnd);
        etDndMinImportance = findViewById(R.id.etDndMinImportance);
        etImportantThreshold = findViewById(R.id.etImportantThreshold);
        etAlertVolume = findViewById(R.id.etAlertVolume);
        switchDnd = findViewById(R.id.switchDnd);
        switchAdFilter = findViewById(R.id.switchAdFilter);
        tvCurrentTheme = findViewById(R.id.tvCurrentTheme);

        MaterialButton btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveSettings());

        MaterialButton btnAddWhitelist = findViewById(R.id.btnAddWhitelist);
        btnAddWhitelist.setOnClickListener(v -> showAppPicker());

        MaterialButton btnAbout = findViewById(R.id.btnAbout);
        btnAbout.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));

        MaterialButton btnAddBlacklist = findViewById(R.id.btnAddBlacklist);
        btnAddBlacklist.setOnClickListener(v -> showBlacklistAppPicker());

        findViewById(R.id.cardNotificationAccess).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        });
    }

    private void initThemeColors() {
        colorViews = new View[]{
                findViewById(R.id.colorBlue),
                findViewById(R.id.colorGreen),
                findViewById(R.id.colorPurple),
                findViewById(R.id.colorOrange),
                findViewById(R.id.colorRed),
        };

        selectedThemeColor = prefs.getThemeColor();

        for (int i = 0; i < colorViews.length; i++) {
            final int index = i;
            final String color = THEME_COLORS[i][0];
            final String name = THEME_COLORS[i][1];

            applyColorCircle(colorViews[i], color, color.equalsIgnoreCase(selectedThemeColor));

            colorViews[i].setOnClickListener(v -> {
                selectedThemeColor = color;
                for (int j = 0; j < colorViews.length; j++) {
                    applyColorCircle(colorViews[j], THEME_COLORS[j][0],
                            THEME_COLORS[j][0].equalsIgnoreCase(selectedThemeColor));
                }
                tvCurrentTheme.setText("当前：" + name);
            });
        }

        // Set initial label
        for (String[] tc : THEME_COLORS) {
            if (tc[0].equalsIgnoreCase(selectedThemeColor)) {
                tvCurrentTheme.setText("当前：" + tc[1]);
                break;
            }
        }
    }

    private void applyColorCircle(View view, String colorHex, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor(colorHex));
        if (selected) {
            drawable.setStroke(6, Color.WHITE);
        } else {
            drawable.setStroke(2, Color.parseColor("#40FFFFFF"));
        }
        view.setBackground(drawable);
    }

    private void loadSettings() {
        etCustomPrompt.setText(prefs.getCustomPrompt());

        switchDnd.setChecked(prefs.isDndMode());
        etDndStart.setText(String.valueOf(prefs.getDndStartHour()));
        etDndEnd.setText(String.valueOf(prefs.getDndEndHour()));
        etDndMinImportance.setText(String.valueOf(prefs.getDndMinImportance()));
        switchAdFilter.setChecked(prefs.isSmartAdFilter());
        etImportantThreshold.setText(String.valueOf(prefs.getImportantThreshold()));
        etAlertVolume.setText(String.valueOf(prefs.getAlertVolume()));

        updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        TextView tvStatus = findViewById(R.id.tvPermissionStatus);
        if (isNotificationListenerEnabled()) {
            tvStatus.setText("✓ 已开启");
            tvStatus.setTextColor(getResources().getColor(R.color.category_life, null));
        } else {
            tvStatus.setText("✗ 未开启 (点击前往设置)");
            tvStatus.setTextColor(getResources().getColor(R.color.importance_critical, null));
        }
    }

    private void saveSettings() {
        String prompt = getText(etCustomPrompt);
        String dndStartStr = getText(etDndStart);
        String dndEndStr = getText(etDndEnd);
        String dndMinStr = getText(etDndMinImportance);
        String thresholdStr = getText(etImportantThreshold);
        String volumeStr = getText(etAlertVolume);

        prefs.setCustomPrompt(prompt);
        prefs.setDndMode(switchDnd.isChecked());
        prefs.setDndStartHour(parseIntSafe(dndStartStr, 22, 0, 23));
        prefs.setDndEndHour(parseIntSafe(dndEndStr, 8, 0, 23));
        prefs.setDndMinImportance(parseIntSafe(dndMinStr, 7, 1, 10));
        prefs.setSmartAdFilter(switchAdFilter.isChecked());
        prefs.setImportantThreshold(parseIntSafe(thresholdStr, 5, 1, 10));
        prefs.setAlertVolume(parseIntSafe(volumeStr, 80, 0, 100));

        String oldThemeColor = prefs.getThemeColor();
        if (selectedThemeColor != null) {
            prefs.setThemeColor(selectedThemeColor);
        }

        WorkScheduler.scheduleSummaryWork(this);

        boolean themeChanged = selectedThemeColor != null && !selectedThemeColor.equalsIgnoreCase(oldThemeColor);
        if (themeChanged) {
            Toast.makeText(this, "设置已保存，正在应用新主题…", Toast.LENGTH_SHORT).show();
            finish();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private int parseIntSafe(String s, int defaultVal, int min, int max) {
        try {
            int val = Integer.parseInt(s);
            return Math.max(min, Math.min(max, val));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private void setupWhitelist() {
        RecyclerView recyclerWhitelist = findViewById(R.id.recyclerWhitelist);
        whitelistAdapter = new WhitelistAdapter();
        recyclerWhitelist.setLayoutManager(new LinearLayoutManager(this));
        recyclerWhitelist.setAdapter(whitelistAdapter);

        whitelistAdapter.setOnRemoveListener(app -> {
            Executors.newSingleThreadExecutor().execute(() -> db.whitelistDao().delete(app));
        });

        db.whitelistDao().getAll().observe(this, apps -> {
            if (apps != null) whitelistAdapter.setApps(apps);
        });
    }

    private void showAppPicker() {
        loadInstalledApps((names, filteredApps) -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("选择应用")
                    .setItems(names, (dialog, which) -> {
                        ApplicationInfo selected = filteredApps.get(which);
                        PackageManager pm = getPackageManager();
                        WhitelistApp app = new WhitelistApp(
                                selected.packageName,
                                pm.getApplicationLabel(selected).toString(),
                                "用户添加"
                        );
                        Executors.newSingleThreadExecutor().execute(() -> db.whitelistDao().insert(app));
                    })
                    .show();
        });
    }

    private void loadInstalledApps(AppListCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<String> appNames = new ArrayList<>();
            List<ApplicationInfo> filteredApps = new ArrayList<>();

            String myPkg = getPackageName();

            for (ApplicationInfo appInfo : allApps) {
                if (myPkg.equals(appInfo.packageName)) continue;

                boolean isUserApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
                boolean isUpdatedSystem = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
                boolean hasLauncher = pm.getLaunchIntentForPackage(appInfo.packageName) != null;

                if (isUserApp || isUpdatedSystem || hasLauncher) {
                    String label = pm.getApplicationLabel(appInfo).toString();
                    filteredApps.add(appInfo);
                    appNames.add(label + "  (" + appInfo.packageName + ")");
                }
            }

            java.util.List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < appNames.size(); i++) indices.add(i);
            indices.sort((a, b) -> appNames.get(a).compareToIgnoreCase(appNames.get(b)));

            List<String> sortedNames = new ArrayList<>();
            List<ApplicationInfo> sortedApps = new ArrayList<>();
            for (int idx : indices) {
                sortedNames.add(appNames.get(idx));
                sortedApps.add(filteredApps.get(idx));
            }

            runOnUiThread(() -> callback.onAppsLoaded(
                    sortedNames.toArray(new String[0]), sortedApps));
        });
    }

    private interface AppListCallback {
        void onAppsLoaded(String[] names, List<ApplicationInfo> apps);
    }

    private void setupBlacklist() {
        RecyclerView recyclerBlacklist = findViewById(R.id.recyclerBlacklist);
        blacklistAdapter = new BlacklistAdapter();
        recyclerBlacklist.setLayoutManager(new LinearLayoutManager(this));
        recyclerBlacklist.setAdapter(blacklistAdapter);

        blacklistAdapter.setOnRemoveListener(app -> {
            Executors.newSingleThreadExecutor().execute(() -> db.blacklistDao().delete(app));
        });

        db.blacklistDao().getAll().observe(this, apps -> {
            if (apps != null) blacklistAdapter.setApps(apps);
        });
    }

    private void showBlacklistAppPicker() {
        loadInstalledApps((names, filteredApps) -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("选择免分类应用")
                    .setItems(names, (dialog, which) -> {
                        ApplicationInfo selected = filteredApps.get(which);
                        PackageManager pm = getPackageManager();
                        BlacklistApp app = new BlacklistApp(
                                selected.packageName,
                                pm.getApplicationLabel(selected).toString()
                        );
                        Executors.newSingleThreadExecutor().execute(() -> db.blacklistDao().insert(app));
                    })
                    .show();
        });
    }

    private boolean isNotificationListenerEnabled() {
        ComponentName cn = new ComponentName(this, NotificationListener.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }
}
