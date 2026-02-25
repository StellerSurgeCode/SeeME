package com.zjf.seeme.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "todos")
public class TodoEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long messageId;
    private String title;
    private String content;
    private String fromApp;
    private long createdAt;
    private long dueAt;
    private boolean isDone;
    private int priority; // 1-5

    public TodoEntity() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFromApp() { return fromApp; }
    public void setFromApp(String fromApp) { this.fromApp = fromApp; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getDueAt() { return dueAt; }
    public void setDueAt(long dueAt) { this.dueAt = dueAt; }

    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}
