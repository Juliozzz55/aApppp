package com.example.simplecalendar.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.simplecalendar.data.Event
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorDialog(
    target: EditorTarget,
    onSave: (id: Long, title: String, epochDay: Long, startMinute: Int, notes: String) -> Unit,
    onDelete: (Event) -> Unit,
    onDismiss: () -> Unit
) {
    val existing = target.event

    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var epochDay by rememberSaveable { mutableStateOf(existing?.dateEpochDay ?: target.date.toEpochDay()) }
    var minute by rememberSaveable { mutableStateOf(existing?.startMinute ?: (9 * 60)) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var titleError by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New event" else "Edit event") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(100)
                        titleError = false
                    },
                    label = { Text("Title") },
                    singleLine = true,
                    isError = titleError,
                    supportingText = if (titleError) ({ Text("Please enter a title") }) else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Date: " + LocalDate.ofEpochDay(epochDay)
                            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    )
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start time: " + formatMinute(minute))
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(500) },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                if (existing != null) {
                    TextButton(
                        onClick = { onDelete(existing) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete event")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    titleError = true
                } else {
                    onSave(existing?.id ?: 0L, title.trim(), epochDay, minute, notes.trim())
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(epochDay)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        epochDay = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
        val timeState = rememberTimePickerState(
            initialHour = minute / 60,
            initialMinute = minute % 60,
            is24Hour = is24Hour
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Start time") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    minute = timeState.hour * 60 + timeState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
