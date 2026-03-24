package com.example.visualreminderlauncher

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.example.visualreminderlauncher.ui.theme.VisualReminderTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VisualReminderTheme {
                val context = LocalContext.current
                val view = LocalView.current
                
                // Load and sort apps
                val allApps = remember { 
                    val rawApps = getInstalledApps()
                    val savedOrder = UserPreferences.getAppOrder(context)
                    val sorted = if (savedOrder != null) {
                        val existing = rawApps.filter { savedOrder.contains(it.packageName) }
                            .sortedBy { savedOrder.indexOf(it.packageName) }
                            .toMutableList()
                        val newApps = rawApps.filter { !savedOrder.contains(it.packageName) }
                        existing.addAll(newApps)
                        existing
                    } else {
                        rawApps.sortedBy { it.name }
                    }
                    sorted.toMutableStateList()
                }

                var reminderActive by remember { mutableStateOf(TimeManager.isReminderTime(context)) }
                var wallpaperUri by remember { mutableStateOf(UserPreferences.getWallpaperUri(context)) }
                var selectedApps by remember { mutableStateOf(UserPreferences.getSelectedApps(context)) }
                var movingAppIndex by remember { mutableStateOf<Int?>(null) }
                var lastClickedPackageName by remember { mutableStateOf<String?>(null) }
                var showReminderDialog by remember { mutableStateOf<String?>(null) }
                var customWarningMessage by remember { mutableStateOf(UserPreferences.getWarningMessage(context)) }
                
                // Clock State
                var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }

                LaunchedEffect(Unit) {
                    while (true) {
                        reminderActive = TimeManager.isReminderTime(context)
                        wallpaperUri = UserPreferences.getWallpaperUri(context)
                        selectedApps = UserPreferences.getSelectedApps(context)
                        customWarningMessage = UserPreferences.getWarningMessage(context)
                        currentTime = Calendar.getInstance().time
                        delay(1000) 
                    }
                }

                LaunchedEffect(showReminderDialog) {
                    if (showReminderDialog != null) {
                        delay(6000)
                        showReminderDialog = null
                    }
                }

                // EXTREME Chaotic Animation
                val infiniteTransition = rememberInfiniteTransition(label = "wallpaperAnimation")
                val scaleAnim by infiniteTransition.animateFloat(
                    initialValue = 1.0f, targetValue = 2.2f,
                    animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
                    label = "wallpaperScale"
                )
                val transXAnim by infiniteTransition.animateFloat(
                    initialValue = -180f, targetValue = 180f,
                    animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
                    label = "wallpaperTransX"
                )
                val transYAnim by infiniteTransition.animateFloat(
                    initialValue = -150f, targetValue = 150f,
                    animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
                    label = "wallpaperTransY"
                )
                val rotationAnim by infiniteTransition.animateFloat(
                    initialValue = -15f, targetValue = 15f,
                    animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse),
                    label = "wallpaperRotation"
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. IMMERSIVE WALLPAPER
                    val wallpaperBitmap = remember(wallpaperUri) {
                        wallpaperUri?.let { uriString ->
                            try {
                                val inputStream = context.contentResolver.openInputStream(Uri.parse(uriString))
                                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                            } catch (e: Exception) { null }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        val imageModifier = Modifier.fillMaxSize().graphicsLayer {
                            if (reminderActive) {
                                scaleX = scaleAnim
                                scaleY = scaleAnim
                                translationX = transXAnim
                                translationY = transYAnim
                                rotationZ = rotationAnim
                            }
                        }
                        if (wallpaperBitmap != null) {
                            Image(bitmap = wallpaperBitmap, contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Crop)
                        } else {
                            Image(painter = painterResource(id = R.drawable.launcher_wallpaper), contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Crop)
                        }
                        
                        // Subtle dark overlay at the bottom for dock readability
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)), startY = 1500f)
                        ))
                    }

                    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                        // 2. STATUS BAR AREA / REMINDER INDICATOR / REORDER BANNER
                        Box(modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                            if (movingAppIndex != null) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "● REORDER MODE: Select target spot",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                        color = Color.Cyan
                                    )
                                    IconButton(onClick = { movingAppIndex = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else if (reminderActive) {
                                Text(
                                    text = "● REMINDER MODE ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = Color.Red
                                )
                            }
                        }

                        // 3. CLOCK & DATE WIDGET
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                            
                            Text(
                                text = timeFormat.format(currentTime),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 72.sp,
                                    color = Color.White,
                                    shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.5f), blurRadius = 8f)
                                )
                            )
                            Text(
                                text = dateFormat.format(currentTime).uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 2.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            )
                        }

                        // 4. PROFESSIONAL SEARCH BAR
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp).height(54.dp).clickable {
                                startActivity(Intent(Intent.ACTION_WEB_SEARCH))
                            },
                            shape = RoundedCornerShape(27.dp),
                            color = Color.White.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(painter = painterResource(id = android.R.drawable.ic_menu_search), contentDescription = null, modifier = Modifier.size(20.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Search your world...", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, modifier = Modifier.weight(1f))
                                Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                            }
                        }

                        // 5. MAIN APP AREA (Pager)
                        val dockSize = 4
                        val pagedApps = allApps.drop(dockSize)
                        val itemsPerPage = 16 // 4x4
                        val pagerState = rememberPagerState(pageCount = { (pagedApps.size + itemsPerPage - 1) / itemsPerPage })

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) { page ->
                            val startIdx = page * itemsPerPage
                            val endIdx = (startIdx + itemsPerPage).coerceAtMost(pagedApps.size)
                            val appsOnPage = pagedApps.subList(startIdx, endIdx)

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                userScrollEnabled = false 
                            ) {
                                itemsIndexed(appsOnPage) { idx, app ->
                                    val currentIdx = dockSize + startIdx + idx
                                    AppIconItem(
                                        app = app,
                                        isMoving = movingAppIndex == currentIdx,
                                        shouldApplyReminder = reminderActive && selectedApps.contains(app.packageName),
                                        isDock = false,
                                        onClick = {
                                            if (movingAppIndex != null) {
                                                if (movingAppIndex != currentIdx) {
                                                    val from = movingAppIndex!!
                                                    val item = allApps.removeAt(from)
                                                    allApps.add(currentIdx, item)
                                                    UserPreferences.saveAppOrder(context, allApps.map { it.packageName })
                                                }
                                                movingAppIndex = null
                                            } else {
                                                handleAppClick(
                                                    context = context,
                                                    app = app,
                                                    reminderActive = reminderActive,
                                                    selectedApps = selectedApps,
                                                    lastClickedPackageName = lastClickedPackageName,
                                                    onStateUpdate = { lastClickedPackageName = it },
                                                    onShowReminder = { showReminderDialog = it }
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            movingAppIndex = currentIdx
                                        }
                                    )
                                }
                            }
                        }

                        // 6. PROFESSIONAL DOCK
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                allApps.take(dockSize).forEachIndexed { idx, app ->
                                    AppIconItem(
                                        app = app,
                                        isMoving = movingAppIndex == idx,
                                        shouldApplyReminder = reminderActive && selectedApps.contains(app.packageName),
                                        isDock = true,
                                        onClick = {
                                            if (movingAppIndex != null) {
                                                if (movingAppIndex != idx) {
                                                    val from = movingAppIndex!!
                                                    val item = allApps.removeAt(from)
                                                    allApps.add(idx, item)
                                                    UserPreferences.saveAppOrder(context, allApps.map { it.packageName })
                                                }
                                                movingAppIndex = null
                                            } else {
                                                handleAppClick(
                                                    context = context,
                                                    app = app,
                                                    reminderActive = reminderActive,
                                                    selectedApps = selectedApps,
                                                    lastClickedPackageName = lastClickedPackageName,
                                                    onStateUpdate = { lastClickedPackageName = it },
                                                    onShowReminder = { showReminderDialog = it }
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            movingAppIndex = idx
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Custom Reminder Dialog
                    if (showReminderDialog != null) {
                        Dialog(
                            onDismissRequest = { showReminderDialog = null },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.8f))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = customWarningMessage.replace("%s", showReminderDialog ?: "App"),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleAppClick(
        context: android.content.Context,
        app: AppInfo,
        reminderActive: Boolean,
        selectedApps: Set<String>,
        lastClickedPackageName: String?,
        onStateUpdate: (String?) -> Unit,
        onShowReminder: (String) -> Unit
    ) {
        if (app.packageName == "com.vr.settings") {
            startActivity(Intent(context, SettingsActivity::class.java))
        } else {
            val isSelectedApp = selectedApps.contains(app.packageName)
            if (reminderActive && isSelectedApp) {
                if (lastClickedPackageName == app.packageName) {
                    launchApp(context, app.packageName)
                    onStateUpdate(null)
                } else {
                    onShowReminder(app.name)
                    onStateUpdate(app.packageName)
                }
            } else {
                launchApp(context, app.packageName)
                onStateUpdate(null)
            }
        }
    }

    private fun launchApp(context: android.content.Context, packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun AppIconItem(
        app: AppInfo,
        isMoving: Boolean,
        shouldApplyReminder: Boolean,
        isDock: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(if (isDock) 75.dp else 85.dp)
                .padding(vertical = 4.dp)
                .graphicsLayer {
                    if (isMoving) {
                        scaleX = 1.3f
                        scaleY = 1.3f
                        alpha = 0.6f
                    }
                }
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val iconSize = if (isDock) 54.dp else 52.dp
                if (app.packageName == "com.vr.settings") {
                    Box(
                        modifier = Modifier.size(iconSize).clip(CircleShape).background(Color(0xFF333333)).shadow(4.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                } else {
                    Image(
                        bitmap = app.icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize).shadow(2.dp, RoundedCornerShape(12.dp))
                    )
                }

                if (shouldApplyReminder) {
                    Image(
                        painter = painterResource(id = R.drawable.reminder_icon),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            if (!isDock) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Medium,
                        shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.8f), blurRadius = 4f)
                    ),
                    maxLines = 1,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp).padding(horizontal = 4.dp).fillMaxWidth()
                )
            }
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val list = mutableListOf<AppInfo>()
        
        list.add(AppInfo("VR Settings", "com.vr.settings", resources.getDrawable(android.R.drawable.ic_menu_preferences, null)))

        for (info in resolveInfos) {
            val name = info.loadLabel(pm).toString()
            val packageName = info.activityInfo.packageName
            val icon = info.loadIcon(pm)
            list.add(AppInfo(name, packageName, icon))
        }
        return list
    }
}
