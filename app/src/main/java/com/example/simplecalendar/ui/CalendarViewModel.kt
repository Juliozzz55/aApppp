package com.example.simplecalendar.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.simplecalendar.data.AppDatabase
import com.example.simplecalendar.data.Event
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** What the event form is showing: [event] == null means "adding a new event". */
data class EditorTarget(val event: Event?, val date: LocalDate)

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).eventDao()

    val events: StateFlow<List<Event>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var visibleMonth by mutableStateOf(YearMonth.now())
        private set

    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    var editor by mutableStateOf<EditorTarget?>(null)
        private set

    fun previousMonth() {
        visibleMonth = visibleMonth.minusMonths(1)
    }

    fun nextMonth() {
        visibleMonth = visibleMonth.plusMonths(1)
    }

    fun goToToday() {
        val today = LocalDate.now()
        selectedDate = today
        visibleMonth = YearMonth.from(today)
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
    }

    fun startAdd() {
        editor = EditorTarget(event = null, date = selectedDate)
    }

    fun startEdit(event: Event) {
        editor = EditorTarget(event = event, date = LocalDate.ofEpochDay(event.dateEpochDay))
    }

    fun closeEditor() {
        editor = null
    }

    fun save(id: Long, title: String, epochDay: Long, startMinute: Int, notes: String) {
        val event = Event(id = id, title = title, dateEpochDay = epochDay, startMinute = startMinute, notes = notes)
        viewModelScope.launch {
            if (id == 0L) dao.insert(event) else dao.update(event)
        }
        // Jump to the saved event's date so the user sees it.
        val date = LocalDate.ofEpochDay(epochDay)
        selectedDate = date
        visibleMonth = YearMonth.from(date)
        editor = null
    }

    fun delete(event: Event) {
        viewModelScope.launch { dao.delete(event) }
        editor = null
    }
}
