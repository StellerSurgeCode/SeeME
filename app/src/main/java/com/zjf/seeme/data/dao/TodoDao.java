package com.zjf.seeme.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.zjf.seeme.data.entity.TodoEntity;

import java.util.List;

@Dao
public interface TodoDao {

    @Insert
    long insert(TodoEntity todo);

    @Update
    void update(TodoEntity todo);

    @Delete
    void delete(TodoEntity todo);

    @Query("SELECT * FROM todos WHERE isDone = 0 ORDER BY priority DESC, createdAt DESC")
    LiveData<List<TodoEntity>> getActiveTodos();

    @Query("SELECT * FROM todos ORDER BY isDone ASC, priority DESC, createdAt DESC")
    LiveData<List<TodoEntity>> getAllTodos();

    @Query("UPDATE todos SET isDone = :done WHERE id = :id")
    void setDone(long id, boolean done);

    @Query("SELECT COUNT(*) FROM todos WHERE isDone = 0")
    LiveData<Integer> getActiveCount();

    @Query("SELECT * FROM todos WHERE isDone = 0 ORDER BY priority DESC, createdAt DESC")
    List<TodoEntity> getActiveTodosSync();

    @Query("DELETE FROM todos")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM todos WHERE isDone = 0")
    int getActiveCountSync();
}
