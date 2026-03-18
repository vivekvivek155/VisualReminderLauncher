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
                var startTimeState by remember { mutableStateOf(TimeManager.getStartTime(context)) }
                var endTimeState by remember { mutableStateOf(TimeManager.getEndTime(context)) }
                var warningMessage by remember { mutableStateOf(UserPreferences.getWarningMessage(context)) }
                var showMessageDialog by remember { mutableStateOf(false) }

                SettingsScreen(
                    startTime = startTimeState,
                    endTime = endTimeState,
                    warningMessage = warningMessage,
                    onSetStartTime = { 
                        pickTime(startTimeState.first, startTimeState.second) { h, m -> 
                            TimeManager.saveStartTime(this, h, m)
                            startTimeState = Pair(h, m)
                        } 
                    },
                    onSetEndTime = { 
                        pickTime(endTimeState.first, endTimeState.second) { h, m -> 
                            TimeManager.saveEndTime(this, h, m)
                            endTimeState = Pair(h, m)
                        } 
                    },
                    onConfirmTiming = {
                        val durationText = calculateDuration(startTimeState, endTimeState)
                        Toast.makeText(this, "Reminder is set for $durationText", Toast.LENGTH_LONG).show()
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

    private fun pickTime(initialHour: Int, initialMinute: Int, callback: (Int, Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, hour, minute -> callback(hour, minute) },
            initialHour,
            initialMinute,
            false 
        ).show()
    }

    private fun calculateDuration(start: Pair<Int, Int>, end: Pair<Int, Int>): String {
        var startMinutes = start.first * 60 + start.second
        var endMinutes = end.first * 60 + end.second
        
        if (endMinutes < startMinutes) {
            endMinutes += 24 * 60 // Overnight
        }
        
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    startTime: Pair<Int, Int>,
    endTime: Pair<Int, Int>,
    warningMessage: String,
    onSetStartTime: () -> Unit,
    onSetEndTime: () -> Unit,
    onConfirmTiming: () -> Unit,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSectionTitle("Reminder Schedule")
            
            SettingsCard {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Schedule,
                        title = "Start Time",
                        subtitle = formatTime(startTime.first, startTime.second),
                        onClick = onSetStartTime
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
                    SettingsRow(
                        icon = Icons.Default.TimerOff,
                        title = "End Time",
                        subtitle = formatTime(endTime.first, endTime.second),
                        onClick = onSetEndTime
                    )
                    
                    Button(
                        onClick = onConfirmTiming,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm Timing")
                    }
                }
            }

            SettingsSectionTitle("Personalization")
            
            SettingsCard {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Wallpaper,
                        title = "Change Wallpaper",
                        subtitle = "Select a custom background image",
                        onClick = onSelectWallpaper
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
                    SettingsRow(
                        icon = Icons.Default.EditNote,
                        title = "Reminder Message",
                        subtitle = warningMessage.replace("%s", "App"),
                        onClick = onEditWarning
                    )
                }
            }

            SettingsSectionTitle("App Control")
            
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.AppRegistration,
                    title = "Restricted Apps",
                    subtitle = "Select apps to restrict during reminder",
                    onClick = onSelectApps
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Visual Reminder Launcher v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 1)
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f)
        )
    }
}

fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
}
