package com.zjf.seeme.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.zjf.seeme.data.entity.BlacklistApp;

import java.util.List;

@Dao
public interface BlacklistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BlacklistApp app);

    @Delete
    void delete(BlacklistApp app);

    @Query("SELECT * FROM blacklist_apps ORDER BY appName ASC")
    LiveData<List<BlacklistApp>> getAll();

    @Query("SELECT * FROM blacklist_apps ORDER BY appName ASC")
    List<BlacklistApp> getAllSync();

    @Query("SELECT EXISTS(SELECT 1 FROM blacklist_apps WHERE packageName = :packageName)")
    boolean isBlacklisted(String packageName);
}
