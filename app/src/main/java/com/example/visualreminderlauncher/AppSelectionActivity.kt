package com.example.visualreminderlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.visualreminderlauncher.ui.theme.VisualReminderTheme

class AppSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VisualReminderTheme {
                AppSelectionScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen() {
    val context = LocalContext.current
    val apps = remember { getInstalledApps(context) }
    var selectedApps by remember { 
        mutableStateOf(UserPreferences.getSelectedApps(context).toMutableSet()) 
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Apps for Reminder") })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(apps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = app.icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = app.name, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = selectedApps.contains(app.packageName),
                        onCheckedChange = { checked ->
                            val newSelected = selectedApps.toMutableSet()
                            if (checked) {
                                newSelected.add(app.packageName)
                            } else {
                                newSelected.remove(app.packageName)
                            }
                            selectedApps = newSelected
                            UserPreferences.saveSelectedApps(context, newSelected)
                        }
                    )
                }
            }
        }
    }
}

private fun getInstalledApps(context: android.content.Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    val list = mutableListOf<AppInfo>()
    for (info in resolveInfos) {
        val name = info.loadLabel(pm).toString()
        val packageName = info.activityInfo.packageName
        val icon = info.loadIcon(pm)
        list.add(AppInfo(name, packageName, icon))
    }
    return list.sortedBy { it.name }
}
