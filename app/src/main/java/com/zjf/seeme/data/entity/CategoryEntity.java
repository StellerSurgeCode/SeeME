package com.zjf.seeme.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "categories")
public class CategoryEntity {

    @PrimaryKey
    @NonNull
    private String name;

    private String icon;
    private int color;
    private boolean isBuiltIn;
    private int sortOrder;

    public CategoryEntity() {
        this.name = "";
    }

    @Ignore
    public CategoryEntity(@NonNull String name, String icon, int color, boolean isBuiltIn, int sortOrder) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.isBuiltIn = isBuiltIn;
        this.sortOrder = sortOrder;
    }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public boolean isBuiltIn() { return isBuiltIn; }
    public void setBuiltIn(boolean builtIn) { isBuiltIn = builtIn; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
