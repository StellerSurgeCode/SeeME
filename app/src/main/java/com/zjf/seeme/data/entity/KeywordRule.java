package com.zjf.seeme.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "keyword_rules")
public class KeywordRule {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String keyword;
    private String matchField; // "title", "content", "sender", "app"
    private int importanceBoost; // -5 to +5
    private String aiHint; // 给 AI 的额外提示
    private boolean enabled;

    public KeywordRule() {
        this.enabled = true;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getMatchField() { return matchField; }
    public void setMatchField(String matchField) { this.matchField = matchField; }

    public int getImportanceBoost() { return importanceBoost; }
    public void setImportanceBoost(int importanceBoost) { this.importanceBoost = importanceBoost; }

    public String getAiHint() { return aiHint; }
    public void setAiHint(String aiHint) { this.aiHint = aiHint; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
