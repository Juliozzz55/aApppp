package com.example.simplecalendar.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.simplecalendar.data.ApiException
import com.example.simplecalendar.data.Event
import com.example.simplecalendar.data.Session
import com.example.simplecalendar.data.SupabaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

/** What the event form is showing: [event] == null means "adding a new event". */
data class EditorTarget(val event: Event?, val date: LocalDate)

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    var session by mutableStateOf<Session?>(loadSession())
        private set

    var busy by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    var visibleMonth by mutableStateOf(YearMonth.now())
        private set

    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    var editor by mutableStateOf<EditorTarget?>(null)
        private set

    init {
        if (session != null) reload()
    }

    // ---------- Session handling ----------

    private fun loadSession(): Session? {
        val access = prefs.getString("access", null)
        val refresh = prefs.getString("refresh", null)
        val email = prefs.getString("email", null)
        return if (access != null && refresh != null && email != null) Session(access, refresh, email) else null
    }

    private fun storeSession(s: Session) {
        prefs.edit()
            .putString("access", s.accessToken)
            .putString("refresh", s.refreshToken)
            .putString("email", s.email)
            .apply()
        session = s
    }

    fun clearMessage() {
        message = null
    }

    fun signOut() {
        prefs.edit().clear().apply()
        session = null
        _events.value = emptyList()
        editor = null
    }

    private fun showError(e: Exception) {
        if (e is ApiException && e.code == 401) {
            signOut()
            message = "Your session expired. Please sign in again."
        } else if (e is IOException) {
            message = "Network error. Please check your internet connection."
        } else {
            message = e.message ?: "Something went wrong."
        }
    }

    /** Runs [block] with a valid access token, refreshing it once if it has expired. */
    private suspend fun <T> authed(block: (String) -> T): T = withContext(Dispatchers.IO) {
        val current = session ?: throw ApiException("Please sign in.", 401)
        try {
            block(current.accessToken)
        } catch (e: ApiException) {
            if (e.code != 401) throw e
            val renewed = SupabaseApi.refresh(current.refreshToken, current.email)
            storeSession(renewed)
            block(renewed.accessToken)
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            message = "Please enter your email and password."
            return
        }
        viewModelScope.launch {
            busy = true
            message = null
            try {
                val s = withContext(Dispatchers.IO) { SupabaseApi.signIn(email, password) }
                storeSession(s)
                reload()
            } catch (e: Exception) {
                showError(e)
            } finally {
                busy = false
            }
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || !email.contains("@")) {
            message = "Please enter a valid email."
            return
        }
        if (password.length < 6) {
            message = "Password must be at least 6 characters."
            return
        }
        viewModelScope.launch {
            busy = true
            message = null
            try {
                val s = withContext(Dispatchers.IO) { SupabaseApi.signUp(email, password) }
                if (s != null) {
                    storeSession(s)
                    reload()
                } else {
                    message = "Account created. Please confirm your email, then sign in."
                }
            } catch (e: Exception) {
                showError(e)
            } finally {
                busy = false
            }
        }
    }

    // ---------- Events (stored in Supabase) ----------

    fun reload() {
        viewModelScope.launch {
            busy = true
            try {
                _events.value = authed { SupabaseApi.listEvents(it) }
            } catch (e: Exception) {
                showError(e)
            } finally {
                busy = false
            }
        }
    }

    fun save(id: Long, title: String, epochDay: Long, startMinute: Int, notes: String) {
        viewModelScope.launch {
            busy = true
            try {
                authed {
                    if (id == 0L) SupabaseApi.insertEvent(it, title, epochDay, startMinute, notes)
                    else SupabaseApi.updateEvent(it, id, title, epochDay, startMinute, notes)
                }
                _events.value = authed { SupabaseApi.listEvents(it) }
            } catch (e: Exception) {
                showError(e)
            } finally {
                busy = false
            }
        }
        // Jump to the saved event's date so the user sees it.
        val date = LocalDate.ofEpochDay(epochDay)
        selectedDate = date
        visibleMonth = YearMonth.from(date)
        editor = null
    }

    fun delete(event: Event) {
        editor = null
        viewModelScope.launch {
            busy = true
            try {
                authed { SupabaseApi.deleteEvent(it, event.id) }
                _events.value = authed { SupabaseApi.listEvents(it) }
            } catch (e: Exception) {
                showError(e)
            } finally {
                busy = false
            }
        }
    }

    // ---------- Calendar navigation ----------

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
}
