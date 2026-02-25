package com.zjf.seeme.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.zjf.seeme.data.entity.WhitelistApp;

import java.util.List;

@Dao
public interface WhitelistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WhitelistApp app);

    @Delete
    void delete(WhitelistApp app);

    @Query("SELECT * FROM whitelist_apps ORDER BY appName ASC")
    LiveData<List<WhitelistApp>> getAll();

    @Query("SELECT * FROM whitelist_apps ORDER BY appName ASC")
    List<WhitelistApp> getAllSync();

    @Query("SELECT EXISTS(SELECT 1 FROM whitelist_apps WHERE packageName = :packageName)")
    boolean isWhitelisted(String packageName);
}
