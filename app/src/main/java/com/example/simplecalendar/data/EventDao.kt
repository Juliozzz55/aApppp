package com.example.simplecalendar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY dateEpochDay ASC, startMinute ASC, title ASC")
    fun observeAll(): Flow<List<Event>>

    @Insert
    suspend fun insert(event: Event)

    @Update
    suspend fun update(event: Event)

    @Delete
    suspend fun delete(event: Event)
}
