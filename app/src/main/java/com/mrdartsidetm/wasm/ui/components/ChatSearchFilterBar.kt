package com.mrdartsidetm.wasm.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mrdartsidetm.wasm.ui.surfaceContainer
import com.mrdartsidetm.wasm.util.ChatDateTimeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Shared Date / Date-Range / Exact-Time / Time-Range filter chip strip for in-chat search
 * across both WhatsApp and Instagram chats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSearchFilterBar(
    filter: ChatDateTimeFilter,
    resultCount: Int,
    onFilterChange: (ChatDateTimeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExactDatePicker by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var timePickerMode by remember { mutableStateOf<String?>(null) } // "exact", "start", "end"

    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    if (showExactDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = filter.startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showExactDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = state.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        onFilterChange(filter.copy(startDate = picked, endDate = picked))
                        showExactDatePicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showExactDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showDateRangePicker) {
        val rangeState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = filter.startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            initialSelectedEndDateMillis = filter.endDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startD = rangeState.selectedStartDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        val endD = rangeState.selectedEndDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        } ?: startD
                        onFilterChange(filter.copy(startDate = startD, endDate = endD))
                        showDateRangePicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = rangeState,
                modifier = Modifier.height(420.dp)
            )
        }
    }

    timePickerMode?.let { mode ->
        val seedTime = when (mode) {
            "end" -> filter.endTime ?: filter.startTime ?: LocalTime.of(23, 59)
            else -> filter.startTime ?: LocalTime.now()
        }
        val timeState = rememberTimePickerState(
            initialHour = seedTime.hour,
            initialMinute = seedTime.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { timePickerMode = null },
            title = {
                Text(
                    text = when (mode) {
                        "exact" -> "Pick Exact Time"
                        "start" -> "Pick Start Time"
                        else -> "Pick End Time"
                    }
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chosen = LocalTime.of(timeState.hour, timeState.minute)
                        when (mode) {
                            "exact" -> {
                                onFilterChange(filter.copy(startTime = chosen, endTime = null))
                                timePickerMode = null
                            }
                            "start" -> {
                                onFilterChange(filter.copy(startTime = chosen, endTime = filter.endTime ?: LocalTime.of(23, 59)))
                                timePickerMode = "end"
                            }
                            else -> {
                                onFilterChange(filter.copy(endTime = chosen))
                                timePickerMode = null
                            }
                        }
                    }
                ) {
                    Text(if (mode == "start") "Next: End Time" else "Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { timePickerMode = null }) { Text("Cancel") }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "$resultCount found",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }

            // Exact Day chip
            val isExactDay = filter.startDate != null && filter.endDate == filter.startDate
            FilterChip(
                selected = isExactDay,
                onClick = { showExactDatePicker = true },
                label = {
                    Text(
                        if (isExactDay) filter.startDate!!.format(dateFormatter) else "Day"
                    )
                },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = if (isExactDay) {
                    {
                        IconButton(
                            onClick = { onFilterChange(filter.copy(startDate = null, endDate = null)) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear day", modifier = Modifier.size(14.dp))
                        }
                    }
                } else null
            )

            // Date Range chip
            val isDateRange = filter.startDate != null && filter.endDate != null && filter.endDate != filter.startDate
            FilterChip(
                selected = isDateRange,
                onClick = { showDateRangePicker = true },
                label = {
                    Text(
                        if (isDateRange) {
                            "${filter.startDate!!.format(dateFormatter)} – ${filter.endDate!!.format(dateFormatter)}"
                        } else {
                            "Date Range"
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = if (isDateRange) {
                    {
                        IconButton(
                            onClick = { onFilterChange(filter.copy(startDate = null, endDate = null)) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear date range", modifier = Modifier.size(14.dp))
                        }
                    }
                } else null
            )

            // Exact Time chip
            val isExactTime = filter.startTime != null && filter.endTime == null
            FilterChip(
                selected = isExactTime,
                onClick = { timePickerMode = "exact" },
                label = {
                    Text(if (isExactTime) filter.startTime!!.format(timeFormatter) else "Time")
                },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = if (isExactTime) {
                    {
                        IconButton(
                            onClick = { onFilterChange(filter.copy(startTime = null, endTime = null)) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear time", modifier = Modifier.size(14.dp))
                        }
                    }
                } else null
            )

            // Time Range chip
            val isTimeRange = filter.startTime != null && filter.endTime != null
            FilterChip(
                selected = isTimeRange,
                onClick = { timePickerMode = "start" },
                label = {
                    Text(
                        if (isTimeRange) {
                            "${filter.startTime!!.format(timeFormatter)} – ${filter.endTime!!.format(timeFormatter)}"
                        } else {
                            "Time Range"
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = if (isTimeRange) {
                    {
                        IconButton(
                            onClick = { onFilterChange(filter.copy(startTime = null, endTime = null)) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear time range", modifier = Modifier.size(14.dp))
                        }
                    }
                } else null
            )
        }
    }
}
