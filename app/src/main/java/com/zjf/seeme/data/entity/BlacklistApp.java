package com.zjf.seeme.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "blacklist_apps")
public class BlacklistApp {

    @PrimaryKey
    @NonNull
    private String packageName;

    private String appName;

    public BlacklistApp() {
        this.packageName = "";
    }

    @Ignore
    public BlacklistApp(@NonNull String packageName, String appName) {
        this.packageName = packageName;
        this.appName = appName;
    }

    @NonNull
    public String getPackageName() { return packageName; }
    public void setPackageName(@NonNull String packageName) { this.packageName = packageName; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
}
