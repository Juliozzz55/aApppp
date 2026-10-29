package com.example.simplecalendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A calendar event.
 * [dateEpochDay] is LocalDate.toEpochDay(); [startMinute] is minutes after midnight (0..1439).
 */
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateEpochDay: Long,
    val startMinute: Int,
    val notes: String = ""
)
