package com.example.simplecalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simplecalendar.data.Event
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: CalendarViewModel = viewModel()) {
    val events by vm.events.collectAsState()
    val today = LocalDate.now()
    val month = vm.visibleMonth
    val selected = vm.selectedDate

    val datesWithEvents = remember(events) { events.map { it.dateEpochDay }.toSet() }
    val selectedEvents = remember(events, selected) {
        events.filter { it.dateEpochDay == selected.toEpochDay() }
    }
    val upcoming = remember(events, today) {
        events.filter { it.dateEpochDay >= today.toEpochDay() }.take(15)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { vm.startAdd() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("+", fontSize = 28.sp)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1 + 2: App name and "Today" button
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Simple Calendar",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(onClick = { vm.goToToday() }) { Text("Today") }
                }
            }

            // 3: Previous / next month
            item(key = "monthNav") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.previousMonth() }) { Text("‹", fontSize = 30.sp) }
                    Text(
                        text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { vm.nextMonth() }) { Text("›", fontSize = 30.sp) }
                }
            }

            // 4: Monthly grid
            item(key = "grid") {
                MonthGrid(
                    month = month,
                    today = today,
                    selected = selected,
                    datesWithEvents = datesWithEvents,
                    onSelect = { vm.selectDate(it) }
                )
            }

            // 5: Selected date
            item(key = "selectedHeader") {
                Text(
                    text = selected.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (selectedEvents.isEmpty()) {
                item(key = "selectedEmpty") {
                    EmptyText("No events on this day. Tap + to add one.")
                }
            } else {
                items(selectedEvents, key = { "s${it.id}" }) { event ->
                    EventRow(event = event, showDate = false, onClick = { vm.startEdit(event) })
                }
            }

            // 6: Upcoming events
            item(key = "upcomingHeader") {
                Text(
                    text = "Upcoming events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            if (upcoming.isEmpty()) {
                item(key = "upcomingEmpty") {
                    EmptyText("No upcoming events.")
                }
            } else {
                items(upcoming, key = { "u${it.id}" }) { event ->
                    EventRow(event = event, showDate = true, onClick = { vm.startEdit(event) })
                }
            }
        }
    }

    // 7: Add / edit / delete form
    vm.editor?.let { target ->
        EventEditorDialog(
            target = target,
            onSave = { id, title, epochDay, minute, notes -> vm.save(id, title, epochDay, minute, notes) },
            onDelete = { vm.delete(it) },
            onDismiss = { vm.closeEditor() }
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    datesWithEvents: Set<Long>,
    onSelect: (LocalDate) -> Unit
) {
    val locale = Locale.getDefault()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val firstOfMonth = month.atDay(1)
    val leadingBlanks = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val daysInMonth = month.lengthOfMonth()
    val rows = (leadingBlanks + daysInMonth + 6) / 7

    Column(modifier = Modifier.fillMaxWidth()) {
        // Weekday labels
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                Text(
                    text = firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp)
                )
            }
        }

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayNumber = row * 7 + col - leadingBlanks + 1
                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val date = month.atDay(dayNumber)
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isSelected = date == selected,
                            hasEvents = date.toEpochDay() in datesWithEvents,
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        isSelected -> colors.primary
        isToday -> colors.primaryContainer
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val textColor = when {
        isSelected -> colors.onPrimary
        isToday -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    val dotColor = if (isSelected) colors.onPrimary else colors.primary

    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (hasEvents) dotColor else androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventRow(event: Event, showDate: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(84.dp)) {
                Text(
                    text = formatMinute(event.startMinute),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (showDate) {
                    Text(
                        text = LocalDate.ofEpochDay(event.dateEpochDay)
                            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (event.notes.isNotBlank()) {
                    Text(
                        text = event.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun formatMinute(minuteOfDay: Int): String {
    val safe = minuteOfDay.coerceIn(0, 24 * 60 - 1)
    return LocalTime.of(safe / 60, safe % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}
