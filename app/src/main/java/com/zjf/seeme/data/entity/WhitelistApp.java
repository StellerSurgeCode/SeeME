package com.zjf.seeme.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "whitelist_apps")
public class WhitelistApp {

    @PrimaryKey
    @NonNull
    private String packageName;

    private String appName;
    private String reason;

    public WhitelistApp() {
        this.packageName = "";
    }

    @Ignore
    public WhitelistApp(@NonNull String packageName, String appName, String reason) {
        this.packageName = packageName;
        this.appName = appName;
        this.reason = reason;
    }

    @NonNull
    public String getPackageName() { return packageName; }
    public void setPackageName(@NonNull String packageName) { this.packageName = packageName; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
