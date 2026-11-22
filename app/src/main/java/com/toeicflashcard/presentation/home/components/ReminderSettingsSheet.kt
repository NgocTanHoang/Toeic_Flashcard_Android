package com.toeicflashcard.presentation.home.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.toeicflashcard.BuildConfig
import com.toeicflashcard.core.notification.ReminderNotificationHelper
import com.toeicflashcard.core.update.GitHubUpdateChecker
import com.toeicflashcard.core.update.UpdateCheckResult
import com.toeicflashcard.core.ui.glass.FocusedContentMaxWidth
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassDialogSurface
import com.toeicflashcard.core.ui.glass.LiquidGlassSheetSurface
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalGlassOpacity
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.UserPreferences
import dev.chrisbanes.haze.HazeState
import java.util.Locale
import kotlin.math.roundToInt

private val SheetRowCardShape = RoundedCornerShape(20.dp)
private val PermissionNoticeShape = RoundedCornerShape(18.dp)

private val ThemeModeOptions = listOf(
    ThemeMode.SYSTEM to "Hệ thống",
    ThemeMode.LIGHT to "Sáng",
    ThemeMode.DARK to "Tối"
)

/**
 * In-window Liquid Glass Settings BottomSheet sharing parent [HazeState] so [hazeChild]
 * applies real-time 20.dp Gaussian frosted blur directly over underlying HomeScreen content.
 */
@Composable
fun ReminderSettingsSheet(
    visible: Boolean,
    hazeState: HazeState,
    themeMode: ThemeMode,
    darkTone: DarkTone,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    enableHapticFeedback: Boolean = false,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDarkTone: (DarkTone) -> Unit,
    onPreviewGlassOpacity: (Float) -> Unit,
    onCommitGlassOpacity: (Float) -> Unit,
    onResetGlassOpacity: () -> Unit,
    onSetHapticFeedback: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onSaveReminderSettings: (enabled: Boolean, hour: Int, minute: Int) -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    var showTimePickerDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = visible) {
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(glassColors.sheetScrim)
                    .semantics { contentDescription = "Đóng cài đặt" }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            ) { it } + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReminderSettingsSheetContent(
                hazeState = hazeState,
                themeMode = themeMode,
                darkTone = darkTone,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                enableHapticFeedback = enableHapticFeedback,
                onSelectThemeMode = onSelectThemeMode,
                onSelectDarkTone = onSelectDarkTone,
                onPreviewGlassOpacity = onPreviewGlassOpacity,
                onCommitGlassOpacity = onCommitGlassOpacity,
                onResetGlassOpacity = onResetGlassOpacity,
                onSetHapticFeedback = onSetHapticFeedback,
                onOpenTimePicker = { showTimePickerDialog = true },
                onDismiss = onDismiss,
                onSaveReminderSettings = onSaveReminderSettings
            )
        }
    }

    if (showTimePickerDialog) {
        ReminderTimePickerDialog(
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            onDismiss = { showTimePickerDialog = false },
            onSaveReminderSettings = onSaveReminderSettings
        )
    }
}

@Composable
private fun ReminderSettingsSheetContent(
    hazeState: HazeState,
    themeMode: ThemeMode,
    darkTone: DarkTone,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    enableHapticFeedback: Boolean,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDarkTone: (DarkTone) -> Unit,
    onPreviewGlassOpacity: (Float) -> Unit,
    onCommitGlassOpacity: (Float) -> Unit,
    onResetGlassOpacity: () -> Unit,
    onSetHapticFeedback: (Boolean) -> Unit,
    onOpenTimePicker: () -> Unit,
    onDismiss: () -> Unit,
    onSaveReminderSettings: (enabled: Boolean, hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current
    val defaultOpacity = UserPreferences.defaultGlassOpacity(glassColors.isDark)
    var permissionDeniedNotice by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateCheckResult by remember { mutableStateOf<UpdateCheckResult?>(null) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                ReminderNotificationHelper.hasNotificationPermission(context)
            ) {
                permissionDeniedNotice = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var sliderValue by remember { mutableFloatStateOf(glassOpacity) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    LaunchedEffect(glassOpacity, isDraggingSlider) {
        if (!isDraggingSlider) {
            sliderValue = glassOpacity
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNotice = false
            onSaveReminderSettings(true, reminderHour, reminderMinute)
        } else {
            permissionDeniedNotice = true
            onSaveReminderSettings(false, reminderHour, reminderMinute)
        }
    }

    val rowGlassFill = glassColors.surfaceFill
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    LiquidGlassSheetSurface(
        hazeState = hazeState,
        blurRadius = 20.dp,
        sheetMaxWidth = FocusedContentMaxWidth,
        modifier = Modifier
            .widthIn(max = FocusedContentMaxWidth)
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0f) {
                            accumulatedDrag += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (accumulatedDrag > 160f) {
                            onDismiss()
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cài đặt",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Section 1: Theme Mode + Collapsible Advanced Appearance (7 Dark Presets & Glass Opacity)
            AppearanceSettingsCard(
                themeMode = themeMode,
                darkTone = darkTone,
                sliderValue = sliderValue,
                defaultOpacity = defaultOpacity,
                containerColor = rowGlassFill,
                onSelectThemeMode = onSelectThemeMode,
                onSelectDarkTone = onSelectDarkTone,
                onPreviewGlassOpacity = { next ->
                    isDraggingSlider = true
                    sliderValue = next
                    onPreviewGlassOpacity(next)
                },
                onCommitGlassOpacity = {
                    isDraggingSlider = false
                    onCommitGlassOpacity(sliderValue)
                },
                onResetGlassOpacity = {
                    isDraggingSlider = false
                    sliderValue = defaultOpacity
                    onResetGlassOpacity()
                }
            )

            // Section 2b: Haptic Feedback Toggle
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = SheetRowCardShape,
                containerColor = rowGlassFill
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Phản hồi rung",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Rung nhẹ khi lật thẻ và đánh giá từ vựng",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = enableHapticFeedback,
                        onCheckedChange = onSetHapticFeedback
                    )
                }
            }

            // Section 3: Daily Reminder Settings
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = SheetRowCardShape,
                containerColor = rowGlassFill
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Nhắc nhở hàng ngày",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { wantEnable ->
                                if (!wantEnable) {
                                    permissionDeniedNotice = false
                                    onSaveReminderSettings(false, reminderHour, reminderMinute)
                                } else if (ReminderNotificationHelper.hasNotificationPermission(context)) {
                                    permissionDeniedNotice = false
                                    onSaveReminderSettings(true, reminderHour, reminderMinute)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSaveReminderSettings(true, reminderHour, reminderMinute)
                                }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(0.6.dp)
                            .background(glassColors.divider)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Giờ nhắc",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LiquidGlassButton(
                            onClick = onOpenTimePicker,
                            tintColor = if (glassColors.isDark) {
                                Color.White.copy(alpha = 0.10f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            },
                            contentColor = MaterialTheme.colorScheme.primary,
                            borderColor = if (glassColors.isDark) null else MaterialTheme.colorScheme.primary,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (permissionDeniedNotice) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = PermissionNoticeShape,
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    borderBrush = rememberLiquidGlassBorderBrush(
                        glassColors = glassColors,
                        accentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ứng dụng cần quyền Thông báo (Android 13+) để gửi nhắc nhở hàng ngày.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                runCatching { context.startActivity(intent) }.onFailure {
                                    val fallback = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    ).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(fallback)
                                }
                            }
                        ) {
                            Text("Mở Cài đặt thông báo")
                        }
                    }
                }
            }

            // Section 4: Opt-in App Version & GitHub Releases Update Check
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = SheetRowCardShape,
                containerColor = rowGlassFill
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !isCheckingUpdate,
                                role = Role.Button
                            ) {
                                isCheckingUpdate = true
                                coroutineScope.launch {
                                    val result = GitHubUpdateChecker.checkForUpdates()
                                    updateCheckResult = result
                                    isCheckingUpdate = false
                                    if (result is UpdateCheckResult.UpdateAvailable) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.downloadUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        runCatching { context.startActivity(intent) }
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "Lexora v${BuildConfig.VERSION_NAME} • Kiểm tra cập nhật",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "TOEIC Vocabulary • SM-2",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (isCheckingUpdate) {
                            Spacer(modifier = Modifier.width(10.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    when (val result = updateCheckResult) {
                        is UpdateCheckResult.UpdateAvailable -> {
                            Text(
                                text = "Có bản mới v${result.latestVersion} • Chạm để tải APK",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.downloadUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        runCatching { context.startActivity(intent) }
                                    }
                                    .padding(start = 42.dp, end = 16.dp, bottom = 12.dp)
                            )
                        }

                        is UpdateCheckResult.UpToDate -> {
                            Text(
                                text = "Đã là phiên bản mới nhất (v${result.currentVersion})",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(start = 42.dp, end = 16.dp, bottom = 12.dp)
                            )
                        }

                        is UpdateCheckResult.Error -> {
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 42.dp, end = 16.dp, bottom = 12.dp)
                            )
                        }

                        null -> Unit
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun AppearanceSettingsCard(
    themeMode: ThemeMode,
    darkTone: DarkTone,
    sliderValue: Float,
    defaultOpacity: Float,
    containerColor: Color,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDarkTone: (DarkTone) -> Unit,
    onPreviewGlassOpacity: (Float) -> Unit,
    onCommitGlassOpacity: () -> Unit,
    onResetGlassOpacity: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    var showAdvancedAppearance by remember { mutableStateOf(false) }
    val pillTrackColor = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = SheetRowCardShape,
        containerColor = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Giao diện",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Row 1: ThemeMode Segmented Pill (Hệ thống | Sáng | Tối)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(CircleShape)
                    .background(pillTrackColor)
                    .border(0.75.dp, glassColors.defaultBorderBrush, CircleShape)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemeModeOptions.forEach { (mode, label) ->
                    val isSelected = themeMode == mode
                    val segmentBg = when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        else -> Color.Transparent
                    }
                    val segmentFg = when {
                        isSelected && glassColors.isDark -> MaterialTheme.colorScheme.primary
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(segmentBg)
                            .then(
                                if (isSelected) {
                                    Modifier.border(0.8.dp, glassColors.defaultBorderBrush, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .semantics {
                                selected = isSelected
                                contentDescription = "Chế độ hiển thị: $label"
                            }
                            .clickable(role = Role.RadioButton) {
                                onSelectThemeMode(mode)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = segmentFg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Progressive disclosure toggle for 7 DarkTone presets + Glass Opacity slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedAppearance = !showAdvancedAppearance }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tùy chỉnh giao diện nâng cao",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (showAdvancedAppearance) {
                        Icons.Rounded.KeyboardArrowUp
                    } else {
                        Icons.Rounded.KeyboardArrowDown
                    },
                    contentDescription = "Mở rộng tùy chỉnh giao diện nâng cao",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = showAdvancedAppearance) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = DarkTone.entries,
                            key = { it.id }
                        ) { tone ->
                            DarkThemeSwatchPill(
                                tone = tone,
                                isSelected = darkTone == tone,
                                onClick = { onSelectDarkTone(tone) }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Opacity,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Độ đục kính: ${(sliderValue * 100f).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(
                                onClick = onResetGlassOpacity,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Mặc định", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = onPreviewGlassOpacity,
                            onValueChangeFinished = onCommitGlassOpacity,
                            valueRange = UserPreferences.MIN_GLASS_OPACITY..UserPreferences.MAX_GLASS_OPACITY,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = glassColors.hairlineTrack
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkThemeSwatchPill(
    tone: DarkTone,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgDot = remember(tone) { Color(tone.backgroundHex) }
    val accentDot = remember(tone) { Color(tone.accentHex) }
    val primaryText = remember(tone) { Color(tone.primaryTextHex) }
    val pillBg = remember(tone, isSelected) {
        if (isSelected) Color(tone.elevatedSurfaceHex) else Color(tone.glassSurfaceHex)
    }
    val pillBorderColor = remember(tone, isSelected) {
        if (isSelected) {
            Color(tone.accentHex)
        } else {
            Color(tone.borderTintHex).copy(
                alpha = if (tone == DarkTone.GITHUB_DARK) 0.90f else 0.35f
            )
        }
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(pillBg)
            .border(
                width = if (isSelected) 1.6.dp else 0.8.dp,
                color = pillBorderColor,
                shape = CircleShape
            )
            .semantics {
                selected = isSelected
                contentDescription = "Tông tối: ${tone.label}"
            }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(bgDot)
                    .border(
                        width = 1.dp,
                        color = primaryText.copy(alpha = 0.28f),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accentDot)
            )
        }

        Text(
            text = tone.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = primaryText,
            maxLines = 1,
            softWrap = false
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = accentDot,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    onDismiss: () -> Unit,
    onSaveReminderSettings: (enabled: Boolean, hour: Int, minute: Int) -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val timePickerState = rememberTimePickerState(
        initialHour = reminderHour,
        initialMinute = reminderMinute,
        is24Hour = true
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassDialogSurface(
            blurRadius = 20.dp,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Chọn giờ nhắc nhở",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                TimePicker(state = timePickerState)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }
                    LiquidGlassButton(
                        onClick = {
                            onDismiss()
                            onSaveReminderSettings(
                                reminderEnabled,
                                timePickerState.hour,
                                timePickerState.minute
                            )
                        },
                        tintColor = if (glassColors.isDark) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        },
                        contentColor = if (glassColors.isDark) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        borderColor = if (glassColors.isDark) null else MaterialTheme.colorScheme.primary,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("Lưu", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
