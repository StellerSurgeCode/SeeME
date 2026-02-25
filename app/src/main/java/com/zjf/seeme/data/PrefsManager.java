package com.zjf.seeme.data;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {

    private static final String PREFS_NAME = "seeme_prefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_POLL_INTERVAL = "poll_interval";
    private static final String KEY_CUSTOM_PROMPT = "custom_prompt";
    private static final String KEY_THEME_COLOR = "theme_color";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_SUMMARY_COUNT = "summary_count";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_DND_MODE = "dnd_mode";
    private static final String KEY_DND_START_HOUR = "dnd_start_hour";
    private static final String KEY_DND_END_HOUR = "dnd_end_hour";
    private static final String KEY_SMART_AD_FILTER = "smart_ad_filter";
    private static final String KEY_DND_MIN_IMPORTANCE = "dnd_min_importance";
    private static final String KEY_ALERT_VOLUME = "alert_volume";
    private static final String KEY_IMPORTANT_THRESHOLD = "important_threshold";

    private static final int DEFAULT_POLL_INTERVAL = 15; // minutes
    private static final int DEFAULT_SUMMARY_COUNT = 5;
    private static final String DEFAULT_THEME_COLOR = "#4A90D9";

    private final SharedPreferences prefs;

    private static volatile PrefsManager INSTANCE;

    public static PrefsManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PrefsManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PrefsManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private PrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String key) {
        prefs.edit().putString(KEY_API_KEY, key).apply();
    }

    public int getPollInterval() {
        return prefs.getInt(KEY_POLL_INTERVAL, DEFAULT_POLL_INTERVAL);
    }

    public void setPollInterval(int minutes) {
        prefs.edit().putInt(KEY_POLL_INTERVAL, minutes).apply();
    }

    public String getCustomPrompt() {
        return prefs.getString(KEY_CUSTOM_PROMPT, "");
    }

    public void setCustomPrompt(String prompt) {
        prefs.edit().putString(KEY_CUSTOM_PROMPT, prompt).apply();
    }

    public String getThemeColor() {
        return prefs.getString(KEY_THEME_COLOR, DEFAULT_THEME_COLOR);
    }

    public void setThemeColor(String color) {
        prefs.edit().putString(KEY_THEME_COLOR, color).commit();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, true);
    }

    public void setDarkMode(boolean dark) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply();
    }

    public boolean isNotificationEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    public void setNotificationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply();
    }

    public int getSummaryCount() {
        return prefs.getInt(KEY_SUMMARY_COUNT, DEFAULT_SUMMARY_COUNT);
    }

    public void setSummaryCount(int count) {
        prefs.edit().putInt(KEY_SUMMARY_COUNT, count).apply();
    }

    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean first) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, first).apply();
    }

    public boolean isDndMode() {
        return prefs.getBoolean(KEY_DND_MODE, false);
    }

    public void setDndMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DND_MODE, enabled).apply();
    }

    public int getDndStartHour() {
        return prefs.getInt(KEY_DND_START_HOUR, 22);
    }

    public void setDndStartHour(int hour) {
        prefs.edit().putInt(KEY_DND_START_HOUR, hour).apply();
    }

    public int getDndEndHour() {
        return prefs.getInt(KEY_DND_END_HOUR, 8);
    }

    public void setDndEndHour(int hour) {
        prefs.edit().putInt(KEY_DND_END_HOUR, hour).apply();
    }

    public boolean isSmartAdFilter() {
        return prefs.getBoolean(KEY_SMART_AD_FILTER, true);
    }

    public void setSmartAdFilter(boolean enabled) {
        prefs.edit().putBoolean(KEY_SMART_AD_FILTER, enabled).apply();
    }

    public int getDndMinImportance() {
        return prefs.getInt(KEY_DND_MIN_IMPORTANCE, 7);
    }

    public void setDndMinImportance(int importance) {
        prefs.edit().putInt(KEY_DND_MIN_IMPORTANCE, importance).apply();
    }

    public long getAgreementSignedTimestamp() {
        return prefs.getLong("agreement_signed_ts", 0);
    }

    public void setAgreementSignedTimestamp(long ts) {
        prefs.edit().putLong("agreement_signed_ts", ts).apply();
    }

    public int getAlertVolume() {
        return prefs.getInt(KEY_ALERT_VOLUME, 80);
    }

    public void setAlertVolume(int volume) {
        prefs.edit().putInt(KEY_ALERT_VOLUME, Math.max(0, Math.min(100, volume))).apply();
    }

    public int getImportantThreshold() {
        return prefs.getInt(KEY_IMPORTANT_THRESHOLD, 5);
    }

    public void setImportantThreshold(int threshold) {
        prefs.edit().putInt(KEY_IMPORTANT_THRESHOLD, Math.max(1, Math.min(10, threshold))).apply();
    }
}
