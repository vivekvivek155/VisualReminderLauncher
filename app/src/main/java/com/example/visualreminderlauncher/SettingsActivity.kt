package com.example.visualreminderlauncher

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visualreminderlauncher.ui.theme.VisualReminderTheme
import java.util.*

class SettingsActivity : ComponentActivity() {

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {}
                UserPreferences.saveWallpaperUri(this, it.toString())
                Toast.makeText(this, "Wallpaper updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VisualReminderTheme {
                val context = LocalContext.current
                var schedules by remember { mutableStateOf(UserPreferences.getReminderSchedules(context)) }
                var warningMessage by remember { mutableStateOf(UserPreferences.getWarningMessage(context)) }
                var showMessageDialog by remember { mutableStateOf(false) }
                var showAddScheduleDialog by remember { mutableStateOf(false) }

                SettingsScreen(
                    schedules = schedules,
                    warningMessage = warningMessage,
                    onAddSchedule = { showAddScheduleDialog = true },
                    onDeleteSchedule = { schedule ->
                        schedules = schedules.filter { it.id != schedule.id }
                        UserPreferences.saveReminderSchedules(context, schedules)
                    },
                    onToggleSchedule = { schedule, enabled ->
                        schedules = schedules.map { if (it.id == schedule.id) it.copy(enabled = enabled) else it }
                        UserPreferences.saveReminderSchedules(context, schedules)
                    },
                    onSelectWallpaper = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "image/*"
                        }
                        pickImageLauncher.launch(intent)
                    },
                    onSelectApps = {
                        startActivity(Intent(this, AppSelectionActivity::class.java))
                    },
                    onEditWarning = { showMessageDialog = true },
                    onBack = { finish() }
                )

                if (showAddScheduleDialog) {
                    AddScheduleDialog(
                        onDismiss = { showAddScheduleDialog = false },
                        onSave = { startH, startM, endH, endM, daily ->
                            val newSchedule = ReminderSchedule(
                                startHour = startH, startMinute = startM,
                                endHour = endH, endMinute = endM,
                                repeatDaily = daily
                            )
                            schedules = schedules + newSchedule
                            UserPreferences.saveReminderSchedules(context, schedules)
                            showAddScheduleDialog = false
                            
                            val duration = calculateDuration(Pair(startH, startM), Pair(endH, endM))
                            Toast.makeText(context, "Reminder set for $duration", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                if (showMessageDialog) {
                    var tempMessage by remember { mutableStateOf(warningMessage) }
                    AlertDialog(
                        onDismissRequest = { showMessageDialog = false },
                        title = { Text("Edit Warning Message") },
                        text = {
                            Column {
                                Text("Use %s where the app name should appear.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = tempMessage,
                                    onValueChange = { tempMessage = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                UserPreferences.saveWarningMessage(context, tempMessage)
                                warningMessage = tempMessage
                                showMessageDialog = false
                            }) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showMessageDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }

    private fun calculateDuration(start: Pair<Int, Int>, end: Pair<Int, Int>): String {
        var startMinutes = start.first * 60 + start.second
        var endMinutes = end.first * 60 + end.second
        if (endMinutes < startMinutes) endMinutes += 24 * 60
        val totalMinutes = endMinutes - startMinutes
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "$hours hours $mins minutes"
            hours > 0 -> "$hours hours"
            else -> "$mins minutes"
        }
    }
}

@Composable
fun AddScheduleDialog(onDismiss: () -> Unit, onSave: (Int, Int, Int, Int, Boolean) -> Unit) {
    var startH by remember { mutableIntStateOf(20) }
    var startM by remember { mutableIntStateOf(0) }
    var endH by remember { mutableIntStateOf(22) }
    var endM by remember { mutableIntStateOf(0) }
    var repeatDaily by remember { mutableStateOf(true) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = {
                    TimePickerDialog(context, { _, h, m -> startH = h; startM = m }, startH, startM, false).show()
                }) {
                    Text("Start Time: ${formatTime(startH, startM)}")
                }
                TextButton(onClick = {
                    TimePickerDialog(context, { _, h, m -> endH = h; endM = m }, endH, endM, false).show()
                }) {
                    Text("End Time: ${formatTime(endH, endM)}")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = repeatDaily, onCheckedChange = { repeatDaily = it })
                    Text("Repeat Daily")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(startH, startM, endH, endM, repeatDaily) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    schedules: List<ReminderSchedule>,
    warningMessage: String,
    onAddSchedule: () -> Unit,
    onDeleteSchedule: (ReminderSchedule) -> Unit,
    onToggleSchedule: (ReminderSchedule, Boolean) -> Unit,
    onSelectWallpaper: () -> Unit,
    onSelectApps: () -> Unit,
    onEditWarning: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Launcher Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSchedule) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSectionTitle("Reminder Schedules")
            
            if (schedules.isEmpty()) {
                Text("No schedules set. Tap + to add one.", color = Color.Gray, modifier = Modifier.padding(8.dp))
            }

            schedules.forEach { schedule ->
                ScheduleCard(schedule, onDeleteSchedule, onToggleSchedule)
            }

            SettingsSectionTitle("Personalization")
            SettingsCard {
                Column {
                    SettingsRow(Icons.Default.Wallpaper, "Change Wallpaper", "Custom background", onSelectWallpaper)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
                    SettingsRow(Icons.Default.EditNote, "Reminder Message", warningMessage.replace("%s", "App"), onEditWarning)
                }
            }

            SettingsSectionTitle("App Control")
            SettingsCard {
                SettingsRow(Icons.Default.AppRegistration, "Restricted Apps", "Select apps to restrict", onSelectApps)
            }
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: ReminderSchedule,
    onDelete: (ReminderSchedule) -> Unit,
    onToggle: (ReminderSchedule, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${formatTime(schedule.startHour, schedule.startMinute)} - ${formatTime(schedule.endHour, schedule.endMinute)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (schedule.repeatDaily) "Repeats Daily" else "Once",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Switch(checked = schedule.enabled, onCheckedChange = { onToggle(schedule, it) })
            IconButton(onClick = { onDelete(schedule) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        content()
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 1)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray.copy(alpha = 0.5f))
    }
}

fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
}
