package com.juanarton.encnotes.core.data.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.juanarton.encnotes.core.data.source.local.room.entity.AppStateEntity

@Dao
interface AppStateDAO {
    @Query("SELECT * FROM appstate")
    suspend fun getAppState(): AppStateEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = AppStateEntity::class)
    fun insertAppState(appStateEntity: AppStateEntity)
}