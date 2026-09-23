package com.example.jarvisai.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        DocumentEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun documentDao(): DocumentDao

    companion object {
        const val DATABASE_NAME = "jarvis_ai.db"

        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        fun buildDatabase(context: Context): JarvisDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                JarvisDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
