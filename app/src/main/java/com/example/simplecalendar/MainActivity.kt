package com.example.simplecalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.simplecalendar.ui.CalendarScreen
import com.example.simplecalendar.ui.SimpleCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleCalendarTheme {
                CalendarScreen()
            }
        }
    }
}
