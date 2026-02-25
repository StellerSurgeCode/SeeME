package com.zjf.seeme.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.zjf.seeme.data.entity.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    long insert(MessageEntity message);

    @Update
    void update(MessageEntity message);

    @Delete
    void delete(MessageEntity message);

    @Query("SELECT * FROM messages ORDER BY importance DESC, timestamp DESC")
    LiveData<List<MessageEntity>> getAllByImportance();

    @Query("SELECT * FROM messages WHERE category != '广告推销' ORDER BY importance DESC, timestamp DESC")
    LiveData<List<MessageEntity>> getAllExcludeAds();

    @Query("SELECT * FROM messages WHERE category = :category ORDER BY importance DESC, timestamp DESC")
    LiveData<List<MessageEntity>> getByCategory(String category);

    @Query("SELECT * FROM messages WHERE isRead = 0 ORDER BY importance DESC, timestamp DESC")
    LiveData<List<MessageEntity>> getUnread();

    @Query("SELECT * FROM messages WHERE isRead = 0 ORDER BY importance DESC, timestamp DESC")
    List<MessageEntity> getUnreadSync();

    @Query("SELECT * FROM messages ORDER BY importance DESC, timestamp DESC LIMIT :limit")
    List<MessageEntity> getTopMessagesSync(int limit);

    @Query("UPDATE messages SET isRead = 1 WHERE id = :id")
    void markAsRead(long id);

    @Query("UPDATE messages SET category = :category, importance = :importance WHERE id = :id")
    void updateClassification(long id, String category, int importance);

    @Query("SELECT COUNT(*) FROM messages WHERE category = :category AND isRead = 0")
    LiveData<Integer> getUnreadCountByCategory(String category);

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0")
    LiveData<Integer> getTotalUnreadCount();

    @Query("DELETE FROM messages WHERE timestamp < :before")
    void deleteOlderThan(long before);

    @Query("SELECT DISTINCT category FROM messages")
    List<String> getAllCategoriesSync();

    @Query("SELECT * FROM messages WHERE " +
            "(title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' " +
            "OR appName LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%') " +
            "ORDER BY importance DESC, timestamp DESC")
    LiveData<List<MessageEntity>> search(String query);

    @Query("SELECT * FROM messages WHERE " +
            "senderName LIKE '%' || :sender || '%' " +
            "ORDER BY timestamp DESC")
    LiveData<List<MessageEntity>> searchBySender(String sender);

    @Query("SELECT * FROM messages WHERE " +
            "timestamp BETWEEN :startTime AND :endTime " +
            "ORDER BY importance DESC, timestamp DESC")
    LiveData<List<MessageEntity>> searchByTimeRange(long startTime, long endTime);

    @Query("SELECT * FROM messages WHERE id = :id")
    MessageEntity getByIdSync(long id);

    @Query("DELETE FROM messages")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM messages")
    int getTotalCountSync();

    @Query("SELECT COUNT(DISTINCT category) FROM messages")
    int getDistinctCategoryCountSync();

    @Query("SELECT COUNT(*) FROM messages WHERE category = :category")
    int getTotalCountByCategorySync(String category);

    @Query("SELECT COUNT(*) FROM messages WHERE packageName = :packageName " +
            "AND title = :title AND content = :content AND timestamp > :since")
    int countRecentDuplicate(String packageName, String title, String content, long since);

    @Query("DELETE FROM messages WHERE id NOT IN (" +
            "SELECT MIN(id) FROM messages GROUP BY packageName, title, content" +
            ")")
    int deleteDuplicates();

    @Query("SELECT * FROM messages WHERE packageName = :packageName " +
            "AND timestamp > :since ORDER BY timestamp ASC LIMIT :limit")
    List<MessageEntity> getRecentByPackageSync(String packageName, long since, int limit);

    @Query("SELECT * FROM messages WHERE packageName = :packageName " +
            "AND senderName = :sender AND timestamp > :since ORDER BY timestamp ASC LIMIT :limit")
    List<MessageEntity> getRecentBySenderSync(String packageName, String sender, long since, int limit);
}
