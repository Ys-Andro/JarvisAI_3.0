package com.example.jarvisai.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.jarvisai.data.automation.AutomationDao
import com.example.jarvisai.data.automation.AutomationEntity
import com.example.jarvisai.data.local.database.dao.ConversationDao
import com.example.jarvisai.data.local.database.dao.DocumentDao
import com.example.jarvisai.data.local.database.dao.MemoryDao
import com.example.jarvisai.data.local.database.dao.MessageDao
import com.example.jarvisai.data.local.database.entity.ConversationEntity
import com.example.jarvisai.data.local.database.entity.DocumentEntity
import com.example.jarvisai.data.local.database.entity.MemoryEntity
import com.example.jarvisai.data.local.database.entity.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        DocumentEntity::class,
        AutomationEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun documentDao(): DocumentDao
    abstract fun automationDao(): AutomationDao

    companion object {
        const val DATABASE_NAME = "jarvis_ai.db"

        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS automations (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        triggerAt INTEGER NOT NULL,
                        intervalMinutes INTEGER NOT NULL DEFAULT 0,
                        actionJson TEXT NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun buildDatabase(context: Context): JarvisDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                JarvisDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
