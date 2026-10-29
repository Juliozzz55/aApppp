package com.example.simplecalendar.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * >>> EDIT BASE_URL: paste your Supabase "Project URL" (looks like https://abcdxyz.supabase.co) <<<
 * The key below is the PUBLISHABLE key, which is safe to put in an app. Never put a secret/service_role key here.
 */
object SupabaseConfig {
    const val BASE_URL = "https://khtbrcqqsonudwctsxvw.supabase.co/rest/v1/"
    const val API_KEY = "sb_publishable_S5FZFb15bj4ezc_5zNC2rg_NZadQbNx"
}

class ApiException(message: String, val code: Int = 0) : Exception(message)

data class Session(val accessToken: String, val refreshToken: String, val email: String)

/** Tiny Supabase client using only built-in Android classes (no extra libraries). All calls are blocking. */
object SupabaseApi {

    private fun call(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null,
        prefer: String? = null
    ): String {
        if (SupabaseConfig.BASE_URL.contains("YOUR-PROJECT-REF")) {
            throw ApiException("Supabase URL is not set yet. Edit SupabaseApi.kt and paste your Project URL.")
        }
        val conn = URL(SupabaseConfig.BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("apikey", SupabaseConfig.API_KEY)
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            if (prefer != null) conn.setRequestProperty("Prefer", prefer)
            if (body != null) {
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw ApiException(errorMessage(text, code), code)
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(text: String, code: Int): String {
        try {
            val o = JSONObject(text)
            for (key in listOf("msg", "error_description", "message", "error")) {
                val v = o.optString(key, "")
                if (v.isNotBlank()) return v
            }
        } catch (e: Exception) {
            // not JSON, fall through
        }
        return "Request failed ($code)"
    }

    private fun parseSession(text: String, fallbackEmail: String): Session? {
        val o = JSONObject(text)
        val access = o.optString("access_token", "")
        val refresh = o.optString("refresh_token", "")
        if (access.isBlank() || refresh.isBlank()) return null
        val email = o.optJSONObject("user")?.optString("email", "").orEmpty().ifBlank { fallbackEmail }
        return Session(access, refresh, email)
    }

    // ---------- Auth ----------

    fun signIn(email: String, password: String): Session {
        val body = JSONObject().put("email", email).put("password", password).toString()
        val text = call("POST", "/auth/v1/token?grant_type=password", body = body)
        return parseSession(text, email) ?: throw ApiException("Sign in failed.")
    }

    /** Returns a Session if the account is signed in right away, or null if email confirmation is required. */
    fun signUp(email: String, password: String): Session? {
        val body = JSONObject().put("email", email).put("password", password).toString()
        val text = call("POST", "/auth/v1/signup", body = body)
        return parseSession(text, email)
    }

    fun refresh(refreshToken: String, email: String): Session {
        val body = JSONObject().put("refresh_token", refreshToken).toString()
        val text = call("POST", "/auth/v1/token?grant_type=refresh_token", body = body)
        return parseSession(text, email) ?: throw ApiException("Session expired.", 401)
    }

    // ---------- Events ----------

    fun listEvents(token: String): List<Event> {
        val text = call(
            "GET",
            "/rest/v1/events?select=*&order=event_date.asc,start_minute.asc",
            token = token
        )
        val arr = JSONArray(text)
        val result = ArrayList<Event>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                Event(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    dateEpochDay = LocalDate.parse(o.getString("event_date")).toEpochDay(),
                    startMinute = o.getInt("start_minute"),
                    notes = if (o.isNull("notes")) "" else o.getString("notes")
                )
            )
        }
        return result
    }

    private fun eventJson(title: String, epochDay: Long, startMinute: Int, notes: String): String =
        JSONObject()
            .put("title", title)
            .put("event_date", LocalDate.ofEpochDay(epochDay).toString())
            .put("start_minute", startMinute)
            .put("notes", notes)
            .toString()

    fun insertEvent(token: String, title: String, epochDay: Long, startMinute: Int, notes: String) {
        call(
            "POST", "/rest/v1/events", token = token,
            body = eventJson(title, epochDay, startMinute, notes), prefer = "return=minimal"
        )
    }

    fun updateEvent(token: String, id: Long, title: String, epochDay: Long, startMinute: Int, notes: String) {
        call(
            "PATCH", "/rest/v1/events?id=eq.$id", token = token,
            body = eventJson(title, epochDay, startMinute, notes), prefer = "return=minimal"
        )
    }

    fun deleteEvent(token: String, id: Long) {
        call("DELETE", "/rest/v1/events?id=eq.$id", token = token)
    }
}
