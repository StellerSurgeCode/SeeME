package com.zjf.seeme.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.zjf.seeme.data.entity.KeywordRule;

import java.util.List;

@Dao
public interface KeywordRuleDao {

    @Insert
    long insert(KeywordRule rule);

    @Update
    void update(KeywordRule rule);

    @Delete
    void delete(KeywordRule rule);

    @Query("SELECT * FROM keyword_rules ORDER BY id DESC")
    LiveData<List<KeywordRule>> getAll();

    @Query("SELECT * FROM keyword_rules WHERE enabled = 1")
    List<KeywordRule> getEnabledSync();
}
