package com.example.jarvisai.data.automation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations WHERE enabled = 1 ORDER BY triggerAt ASC")
    fun getEnabled(): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AutomationEntity)

    @Query("UPDATE automations SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun delete(id: String)
}
