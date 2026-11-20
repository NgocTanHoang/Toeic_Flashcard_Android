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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.toeicflashcard.core.notification.ReminderNotificationHelper
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
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDarkTone: (DarkTone) -> Unit,
    onPreviewGlassOpacity: (Float) -> Unit,
    onCommitGlassOpacity: (Float) -> Unit,
    onResetGlassOpacity: () -> Unit,
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
                onSelectThemeMode = onSelectThemeMode,
                onSelectDarkTone = onSelectDarkTone,
                onPreviewGlassOpacity = onPreviewGlassOpacity,
                onCommitGlassOpacity = onCommitGlassOpacity,
                onResetGlassOpacity = onResetGlassOpacity,
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
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDarkTone: (DarkTone) -> Unit,
    onPreviewGlassOpacity: (Float) -> Unit,
    onCommitGlassOpacity: (Float) -> Unit,
    onResetGlassOpacity: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onDismiss: () -> Unit,
    onSaveReminderSettings: (enabled: Boolean, hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current
    val defaultOpacity = UserPreferences.defaultGlassOpacity(glassColors.isDark)
    var permissionDeniedNotice by remember { mutableStateOf(false) }

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
        modifier = Modifier
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

            // Section 1: Theme Mode & 7 Dark Presets
            AppearanceSettingsCard(
                themeMode = themeMode,
                darkTone = darkTone,
                containerColor = rowGlassFill,
                onSelectThemeMode = onSelectThemeMode,
                onSelectDarkTone = onSelectDarkTone
            )

            // Section 2: Real-time Glass Opacity Customization
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = SheetRowCardShape,
                containerColor = rowGlassFill
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Opacity,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                            Column {
                                Text(
                                    text = "Độ đục bề mặt kính",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(sliderValue * 100f).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        LiquidGlassButton(
                            onClick = {
                                isDraggingSlider = false
                                sliderValue = defaultOpacity
                                onResetGlassOpacity()
                            },
                            tintColor = if (glassColors.isDark) {
                                Color.White.copy(alpha = 0.10f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            },
                            contentColor = if (glassColors.isDark) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            borderColor = if (glassColors.isDark) null else MaterialTheme.colorScheme.primary,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mặc định",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Text(
                        text = "Kéo để chỉnh độ đục thẻ kính, thanh tiêu đề và hộp thoại theo thời gian thực.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = sliderValue,
                        onValueChange = { next ->
                            isDraggingSlider = true
                            sliderValue = next
                            onPreviewGlassOpacity(next)
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            onCommitGlassOpacity(sliderValue)
                        },
                        valueRange = UserPreferences.MIN_GLASS_OPACITY..UserPreferences.MAX_GLASS_OPACITY,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = glassColors.hairlineTrack
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "45% (Trong suốt)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "95% (Đặc mịn)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 3: Daily Reminder Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
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
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = SheetRowCardShape,
                containerColor = rowGlassFill
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Nhắc nhở ôn tập TOEIC",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tự động kiểm tra từ đến hạn và nhắc giữ chuỗi Streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = SheetRowCardShape,
                containerColor = rowGlassFill
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Giờ nhắc hàng ngày",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LiquidGlassButton(
                        onClick = onOpenTimePicker,
                        tintColor = if (glassColors.isDark) {
                            Color.White.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        },
                        contentColor = if (glassColors.isDark) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        borderColor = if (glassColors.isDark) null else MaterialTheme.colorScheme.primary,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chọn giờ",
                            style = MaterialTheme.typography.labelLarge
                        )
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
    containerColor: Color,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDarkTone: (DarkTone) -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = "Giao diện & Không gian học",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Row 1: ThemeMode Segmented Pill (Hệ thống | Sáng | Tối)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Chế độ hiển thị",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(pillTrackColor)
                        .border(0.75.dp, glassColors.defaultBorderBrush, CircleShape)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemeModeOptions.forEach { (mode, label) ->
                        val isSelected = themeMode == mode
                        val segmentBg = when {
                            isSelected && glassColors.isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
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
            }

            // Row 2: 7 Dark Mode Theme Swatch Pills in a smooth LazyRow
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bảng màu Dark Mode (7 phong cách)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
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
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
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
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(accentDot)
            )
        }

        Text(
            text = tone.label,
            style = MaterialTheme.typography.labelLarge,
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
