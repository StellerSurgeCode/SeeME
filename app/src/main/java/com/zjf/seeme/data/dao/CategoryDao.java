package com.zjf.seeme.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.zjf.seeme.data.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CategoryEntity category);

    @Update
    void update(CategoryEntity category);

    @Delete
    void delete(CategoryEntity category);

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    LiveData<List<CategoryEntity>> getAll();

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    List<CategoryEntity> getAllSync();

    @Query("SELECT * FROM categories WHERE name = :name")
    CategoryEntity getByName(String name);
}
