package com.zjf.seeme.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.zjf.seeme.data.dao.BlacklistDao;
import com.zjf.seeme.data.dao.CategoryDao;
import com.zjf.seeme.data.dao.KeywordRuleDao;
import com.zjf.seeme.data.dao.MessageDao;
import com.zjf.seeme.data.dao.TodoDao;
import com.zjf.seeme.data.dao.WhitelistDao;
import com.zjf.seeme.data.entity.BlacklistApp;
import com.zjf.seeme.data.entity.CategoryEntity;
import com.zjf.seeme.data.entity.KeywordRule;
import com.zjf.seeme.data.entity.MessageEntity;
import com.zjf.seeme.data.entity.TodoEntity;
import com.zjf.seeme.data.entity.WhitelistApp;

@Database(
        entities = {
                MessageEntity.class,
                CategoryEntity.class,
                WhitelistApp.class,
                TodoEntity.class,
                KeywordRule.class,
                BlacklistApp.class
        },
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract MessageDao messageDao();
    public abstract CategoryDao categoryDao();
    public abstract WhitelistDao whitelistDao();
    public abstract TodoDao todoDao();
    public abstract KeywordRuleDao keywordRuleDao();
    public abstract BlacklistDao blacklistDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "seeme_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
