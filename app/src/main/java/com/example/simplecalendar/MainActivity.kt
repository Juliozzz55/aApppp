package com.example.simplecalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simplecalendar.ui.AuthScreen
import com.example.simplecalendar.ui.CalendarScreen
import com.example.simplecalendar.ui.CalendarViewModel
import com.example.simplecalendar.ui.SimpleCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleCalendarTheme {
                val vm: CalendarViewModel = viewModel()
                if (vm.session == null) {
                    AuthScreen(vm)
                } else {
                    CalendarScreen(vm)
                }
            }
        }
    }
}
