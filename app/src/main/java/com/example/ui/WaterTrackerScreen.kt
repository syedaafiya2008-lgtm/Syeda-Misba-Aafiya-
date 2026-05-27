package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WaterTrackerScreen(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preference by viewModel.preferences.collectAsState()
    val logsToday by viewModel.logsToday.collectAsState()

    val totalIntakeMl = logsToday.sumOf { it.amountMl }
    val progressPercent = if (preference.dailyGoalMl > 0) {
        totalIntakeMl.toFloat() / preference.dailyGoalMl.toFloat()
    } else {
        0f
    }

    var isNotificationPermissionGranted by remember {
        mutableStateOf(isHasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationPermissionGranted = isGranted
        viewModel.toggleNotifications(isGranted)
    }

    var showCustomLogDialog by remember { mutableStateOf(false) }
    var showSettingsExpanded by remember { mutableStateOf(false) }

    val currentDateString = remember {
        SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
    }

    // Get latest log today
    val latestLog = remember(logsToday) {
        logsToday.maxByOrNull { it.timestamp }
    }

    val latestLogTimeString = remember(latestLog) {
        latestLog?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))
        } ?: ""
    }

    // Synchronize alarm state with background on start
    LaunchedEffect(preference.isNotificationEnabled, preference.notificationIntervalMinutes) {
        if (preference.isNotificationEnabled && isHasNotificationPermission(context)) {
            com.example.notification.WaterNotificationHelper.scheduleReminderAlarm(
                context,
                preference.notificationIntervalMinutes
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("water_tracker_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. HEADER ROW (Polished Bento Style with Date & Avatar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AquaTrack",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1.0).sp
                        ),
                        color = Color(0xFF001D35) // Deep navy brand color
                    )
                    Text(
                        text = currentDateString,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF44474E)
                    )
                }

                // Initial avatar (Syeda Aafiya -> SA) clickable to expand settings easily!
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showSettingsExpanded = !showSettingsExpanded },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFDEE2EB).copy(alpha = 0.5f), CircleShape)
                            .testTag("toggle_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Notification Settings",
                            tint = Color(0xFF001D35),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD3E4FF))
                            .clickable { showSettingsExpanded = !showSettingsExpanded }
                            .testTag("avatar_icon_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SA",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFF001D35)
                        )
                    }
                }
            }
        }

        // Notification Permission Alert Banner (inline bento)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationPermissionGranted) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
                        .testTag("permission_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFFDBCF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "Notifications blocked",
                                tint = Color(0xFFBA1A1A),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                    text = "Enable Reminders",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1A1C1E)
                            )
                            Text(
                                    text = "Allow push notifications to get hydration alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF44474E)
                            )
                        }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0061A4)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("request_permission_button")
                        ) {
                            Text("Enable", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }
                }
            }
        }

        // 2. HERO PROGRESS BENTO CELL (col-span-2 row-span-3, Rounded 32dp, Blue background)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("visual_progress_card"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD3E4FF) // Matching HTML Bento Hero background color
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat styling
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .padding(20.dp)
                ) {
                    // Absolute Decorative Circle in background top-right (Asymmetric element)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xAAC7FF).copy(alpha = 0.3f),
                            radius = 110.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(size.width + 20.dp.toPx(), -20.dp.toPx())
                        )
                    }

                    // Content
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        WaterWaveAnimatedProgress(
                            progressPercent = progressPercent,
                            modifier = Modifier
                                .size(170.dp)
                                .testTag("wave_progress_canvas")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "$totalIntakeMl of ${preference.dailyGoalMl} ml",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color(0xFF001D35)
                        )

                        Text(
                            text = if (totalIntakeMl >= preference.dailyGoalMl) {
                                "Daily Target Complete! 🎉"
                            } else {
                                "${(progressPercent * 100).coerceAtMost(100f).toInt()}% towards your goal"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF001D35).copy(alpha = 0.7f)
                        )
                    }

                    // Floating Action Button Styled shortcut inside bottom-right corner of hero
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0061A4))
                            .clickable { showCustomLogDialog = true }
                            .testTag("hero_add_water_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log water",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 3. COLLAPSIBLE SETTINGS PANEL (Smooth Bento styling inside expanded settings)
        item {
            AnimatedVisibility(visible = showSettingsExpanded) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
                        .testTag("settings_expanded_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFE8F0FE), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFF0061A4),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Hourly Reminders",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF1A1C1E)
                                )
                            }
                            Switch(
                                checked = preference.isNotificationEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !isHasNotificationPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.toggleNotifications(checked)
                                    }
                                },
                                modifier = Modifier.testTag("notification_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Daily Goal Sizing Slider info block
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Daily Target ML",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF44474E)
                            )
                            Text(
                                text = "${preference.dailyGoalMl} ml",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF001D35)
                            )
                        }
                        Slider(
                            value = preference.dailyGoalMl.toFloat(),
                            onValueChange = { value ->
                                val step = (value / 100).roundToInt() * 100
                                viewModel.updateDailyGoal(step)
                            },
                            valueRange = 1000f..5000f,
                            modifier = Modifier.testTag("daily_goal_slider")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Notification Interval Option Text Card
                        Text(
                            text = "Reminder Interval Rate",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF44474E),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val intervals = listOf(
                                30 to "30m",
                                60 to "1 Hr",
                                120 to "2 Hr",
                                180 to "3 Hr"
                            )
                            intervals.forEach { (minutes, label) ->
                                val isSelected = preference.notificationIntervalMinutes == minutes
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(0xFF0061A4)
                                            else Color(0xFF0061A4).copy(alpha = 0.08f)
                                        )
                                        .clickable {
                                            viewModel.updateNotificationInterval(minutes)
                                        }
                                        .padding(vertical = 10.dp)
                                        .testTag("interval_chip_$minutes"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else Color(0xFF0061A4)
                                    )
                                }
                            }
                        }

                        // Instant test notification trigger
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.triggerInstantNotification() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trigger_notification_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A1C1E)
                            )
                        ) {
                            Text(
                                text = "⚡ Test Notification Alert Instantly",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 4. TWIN SIDE-BY-SIDE BENTO GLASS CELLS (col-span-1 each)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Goal Card Representation
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
                        .testTag("bento_goal_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "DAILY GOAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            ),
                            color = Color(0xFF44474E)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f L", preference.dailyGoalMl / 1000f),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFF1A1C1E)
                        )
                    }
                }

                // Total Logs Counter Card Representation
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
                        .testTag("bento_logs_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "LOGGED DRINKS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            ),
                            color = Color(0xFF44474E)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${logsToday.size} Serving" + (if (logsToday.size == 1) "" else "s"),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFF1A1C1E)
                        )
                    }
                }
            }
        }

        // 5. PUSH NOTIFICATION CONTROLLER SLATE CELL (col-span-2 row-span-1, background #E1E2EC)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSettingsExpanded = true }
                    .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
                    .testTag("bento_notifications_pill"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE1E2EC) // Soft layout background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (preference.isNotificationEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = Color(0xFF0061A4),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Push Reminders",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1A1C1E)
                            )
                            Text(
                                text = if (preference.isNotificationEnabled) {
                                    "Next alert in ${preference.notificationIntervalMinutes} mins • Always ON"
                                } else {
                                    "Reminders are disabled • Tap to setup"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF44474E)
                            )
                        }
                    }

                    // Simple Indicator dot/circle representing state
                    Box(
                        modifier = Modifier
                            .size(20.dp, 10.dp)
                            .clip(CircleShape)
                            .background(
                                if (preference.isNotificationEnabled) Color(0xFF0061A4)
                                else Color(0xFFBA1A1A)
                            )
                    )
                }
            }
        }

        // 6. LATEST LOG BENTO BOX (col-span-2 row-span-1)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
                    .testTag("bento_latest_drink_box"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Small visual block presenting latest dynamic timestamp
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF0F4F8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LAST",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                ),
                                color = Color(0xFF0061A4)
                            )
                            Text(
                                text = if (latestLog != null) latestLogTimeString else "--:--",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF1A1C1E)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (latestLog != null) {
                                when (latestLog.beverageType.lowercase()) {
                                    "coffee" -> "Brewed Hot Coffee ☕"
                                    "tea" -> "Herbal Chamomile Tea 🍵"
                                    "juice" -> "Squeezed Fresh Juice 🥤"
                                    "soda" -> "Fizzy Carbonated Soda 🥤"
                                    else -> "Pure Mineral Water 💧"
                                }
                            } else {
                                "No Hydration Logged Yet"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1A1C1E)
                        )
                        Text(
                            text = if (latestLog != null) {
                                "${latestLog.amountMl} ml logged • Clean hydration intake"
                            } else {
                                "Sip some water to begin your bento streaks today!"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF44474E)
                        )
                    }
                }
            }
        }

        // 7. QUICK HYDRATION DRINK BOARD
        item {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Quick Hydration Intake",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF001D35),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAddCard(
                        title = "Glass",
                        amount = 250,
                        icon = Icons.Default.WaterDrop,
                        color = Color(0xFF03A9F4),
                        onClick = { viewModel.logWater(250, "water") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_250")
                    )
                    QuickAddCard(
                        title = "Cup",
                        amount = 200,
                        icon = Icons.Default.LocalCafe,
                        color = Color(0xFF4CAF50),
                        onClick = { viewModel.logWater(200, "tea") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_200")
                    )
                    QuickAddCard(
                        title = "Bottle",
                        amount = 500,
                        icon = Icons.Default.LocalDrink,
                        color = Color(0xFF0061A4),
                        onClick = { viewModel.logWater(500, "water") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_500")
                    )
                    QuickAddCard(
                        title = "Custom",
                        amount = 0,
                        icon = Icons.Default.Add,
                        color = Color(0xFF001D35),
                        onClick = { showCustomLogDialog = true },
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("quick_add_custom")
                    )
                }
            }
        }

        // 8. ALL INTAKE LOGS SUBSECTION
        item {
            Text(
                text = "Today's Log Records",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF001D35),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Individual item loops represented styled visually as clean lists
        if (logsToday.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .testTag("empty_state_view"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = Color(0xFFDEE2EB),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No activities recorded today yet",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF44474E)
                        )
                    }
                }
            }
        } else {
            items(logsToday, key = { it.id }) { log ->
                LogItemRow(
                    log = log,
                    onDelete = { viewModel.deleteLog(log) },
                    modifier = Modifier.testTag("log_item_${log.id}")
                )
            }
        }
    }

    // Custom dialog to log custom intake sizes
    if (showCustomLogDialog) {
        CustomLogDialog(
            onDismiss = { showCustomLogDialog = false },
            onConfirm = { amount, type ->
                viewModel.logWater(amount, type)
                showCustomLogDialog = false
            }
        )
    }
}

// Help query notification permission state
private fun isHasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

// Interactive pulsing and waving Canvas progress indicator
@Composable
fun WaterWaveAnimatedProgress(
    progressPercent: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val cleanPercent = progressPercent.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.4f))
            .border(4.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val fillHeight = height * (1f - cleanPercent)

            val path = Path().apply {
                moveTo(0f, height)
                lineTo(0f, fillHeight)

                val waveAmplitude = 10.dp.toPx()
                val waveFrequency = (2 * Math.PI / width).toFloat()

                for (x in 0..width.toInt() step 5) {
                    val y = fillHeight + waveAmplitude * kotlin.math.sin(waveFrequency * x + waveOffset)
                    lineTo(x.toFloat(), y)
                }
                lineTo(width, fillHeight)
                lineTo(width, height)
                close()
            }

            drawPath(
                path = path,
                color = Color(0xFF0061A4) // Intense Blue Progress Wave Color
            )
        }

        // Inner circle indicators
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = if (cleanPercent >= 0.5f) Color.White else Color(0xFF001D35),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${(cleanPercent * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = if (cleanPercent >= 0.5f) Color.White else Color(0xFF001D35)
            )
            Text(
                text = "HYDRATED",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 1.0.sp
                ),
                color = if (cleanPercent >= 0.5f) Color.White.copy(alpha = 0.8f) else Color(0xFF44474E)
            )
        }
    }
}

// Quick Drink Cards representation with Spring clicks
@Composable
fun QuickAddCard(
    title: String,
    amount: Int,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(95.dp)
            .clickable { onClick() }
            .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1A1C1E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (amount > 0) "$amount ml" else "+ Custom",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF44474E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Log view item representing drank beverage
@Composable
fun LogItemRow(
    log: com.example.data.WaterLog,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { formatter.format(Date(log.timestamp)) }

    val drinkInfo = when (log.beverageType.lowercase()) {
        "coffee" -> Triple("Hot Coffee ☕", Icons.Default.Coffee, Color(0xFF8D6E63))
        "tea" -> Triple("Herbal Tea 🍵", Icons.Default.LocalCafe, Color(0xFF4CAF50))
        "juice" -> Triple("Fresh Juice 🥤", Icons.Default.LocalDrink, Color(0xFFFF9800))
        "soda" -> Triple("Fizzy Soda 🥤", Icons.Default.LocalDrink, Color(0xFF9C27B0))
        else -> Triple("Pure Water 💧", Icons.Default.WaterDrop, Color(0xFF2196F3))
    }
    val drinkName: String = drinkInfo.first
    val icon: ImageVector = drinkInfo.second
    val accentColor: Color = drinkInfo.third

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = drinkName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1C1E)
                    )
                    Text(
                        text = "Logged at $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF44474E)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${log.amountMl} ml",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFF0061A4),
                    modifier = Modifier.padding(end = 6.dp)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_log_${log.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = Color(0xFFBA1A1A).copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Dialog for configuring dynamic intake volume
@Composable
fun CustomLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var mlAmount by remember { mutableStateOf(250f) }
    var selectedBeverageType by remember { mutableStateOf("water") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_log_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Log Custom Intake",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color(0xFF001D35)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Beverage Type Section Selector
                Text(
                    text = "SELECT BEVERAGE TYPE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp
                    ),
                    color = Color(0xFF44474E),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "water" to "Water 💧",
                        "tea" to "Tea 🍵",
                        "coffee" to "Coffee ☕",
                        "juice" to "Juice 🥤"
                    ).forEach { (type, label) ->
                        val isSel = selectedBeverageType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSel) Color(0xFF0061A4)
                                    else Color(0xFF0061A4).copy(alpha = 0.08f)
                                )
                                .clickable { selectedBeverageType = type }
                                .padding(vertical = 10.dp)
                                .testTag("dialog_type_chip_$type"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSel) Color.White else Color(0xFF0061A4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Drink amount volume slider
                Text(
                    text = "${mlAmount.toInt()} ml",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFF1A1C1E)
                )
                Slider(
                    value = mlAmount,
                    onValueChange = { mlAmount = it },
                    valueRange = 50f..1200f,
                    steps = 22, // Steps of 50ml
                    modifier = Modifier.testTag("dialog_size_slider")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_cancel")) {
                        Text("Cancel", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF44474E))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(mlAmount.toInt(), selectedBeverageType) },
                        modifier = Modifier.testTag("dialog_confirm"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                    ) {
                        Text("Add to Bento", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }
            }
        }
    }
}
