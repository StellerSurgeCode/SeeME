package com.zjf.seeme.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String packageName;
    private String appName;
    private String title;
    private String content;
    private String category;
    private int importance; // 1-10
    private long timestamp;
    private boolean isRead;
    private String senderName;
    private String extraInfo;
    private String intentUri; // 原始通知跳转 URI
    private String notificationKey; // 原始通知 key

    public MessageEntity() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getImportance() { return importance; }
    public void setImportance(int importance) { this.importance = importance; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getExtraInfo() { return extraInfo; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }

    public String getIntentUri() { return intentUri; }
    public void setIntentUri(String intentUri) { this.intentUri = intentUri; }

    public String getNotificationKey() { return notificationKey; }
    public void setNotificationKey(String notificationKey) { this.notificationKey = notificationKey; }
}
