package com.toeicflashcard.presentation.vault

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.ui.glass.FocusedContentMaxWidth
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.glass.LiquidGlassSheetSurface
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.core.ui.theme.LocalSm2RatingColors
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import java.util.Locale

private val DetailCardShape = RoundedCornerShape(18.dp)
private val DetailInputShape = RoundedCornerShape(14.dp)
private val DetailPosChips = listOf("n.", "v.", "adj.", "adv.", "phr.")

@Composable
fun WordDetailSheet(
    word: VocabularyWord?,
    hazeState: HazeState,
    onSpeakAtRate: (String, Float) -> Unit,
    onToggleStarred: (VocabularyWord) -> Unit,
    onResetProgress: (VocabularyWord) -> Unit,
    onUpdateCustomWord: suspend (VocabularyWord) -> Boolean,
    onDeleteCustomWord: suspend (Int) -> Boolean,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val visible = word != null

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
                    .semantics { contentDescription = "Đóng chi tiết từ vựng" }
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
            if (word != null) {
                WordDetailSheetContent(
                    word = word,
                    hazeState = hazeState,
                    onSpeakAtRate = onSpeakAtRate,
                    onToggleStarred = onToggleStarred,
                    onResetProgress = onResetProgress,
                    onUpdateCustomWord = onUpdateCustomWord,
                    onDeleteCustomWord = onDeleteCustomWord,
                    onShowMessage = onShowMessage,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun WordDetailSheetContent(
    word: VocabularyWord,
    hazeState: HazeState,
    onSpeakAtRate: (String, Float) -> Unit,
    onToggleStarred: (VocabularyWord) -> Unit,
    onResetProgress: (VocabularyWord) -> Unit,
    onUpdateCustomWord: suspend (VocabularyWord) -> Boolean,
    onDeleteCustomWord: suspend (Int) -> Boolean,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val sm2Colors = LocalSm2RatingColors.current
    val coroutineScope = rememberCoroutineScope()
    val isCustomWord = word.deckId == VocabularyDecks.CUSTOM_USER_ID
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    var isEditing by remember(word.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember(word.id) { mutableStateOf(false) }
    var isSaving by remember(word.id) { mutableStateOf(false) }

    var editWord by remember(word.id, word.word) { mutableStateOf(word.word) }
    var editPhonetic by remember(word.id, word.phonetic) { mutableStateOf(word.phonetic) }
    var editPos by remember(word.id, word.partOfSpeech) { mutableStateOf(word.partOfSpeech) }
    var editMeaningVi by remember(word.id, word.meaningVi) { mutableStateOf(word.meaningVi) }
    var editTopic by remember(word.id, word.topic) { mutableStateOf(word.topic) }
    var editExampleEn by remember(word.id, word.exampleEn) { mutableStateOf(word.exampleEn) }
    var editExampleVi by remember(word.id, word.exampleVi) { mutableStateOf(word.exampleVi) }

    val starColor = if (word.isStarred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
    val isForgotten = VocabularyVaultViewModel.isForgotten(word)
    val (statusLabel, statusBg, statusFg) = when {
        isForgotten -> Triple("Hay quên", sm2Colors.again.container, sm2Colors.again.content)
        word.isLearned -> Triple("Đã thuộc", sm2Colors.easy.container, sm2Colors.easy.content)
        else -> Triple(
            "Chưa thuộc",
            if (glassColors.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.62f),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xóa từ vựng?") },
            text = {
                Text("Bạn có chắc muốn xóa \"${word.word}\" khỏi bộ Từ của tôi? Hành động này không thể hoàn tác.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        coroutineScope.launch {
                            val deleted = onDeleteCustomWord(word.id)
                            if (deleted) {
                                onShowMessage("Đã xóa từ \"${word.word}\"")
                                onDismiss()
                            } else {
                                onShowMessage("Không thể xóa từ này")
                            }
                        }
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    LiquidGlassSheetSurface(
        hazeState = hazeState,
        modifier = Modifier
            .widthIn(max = FocusedContentMaxWidth)
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (accumulatedDrag > 120f) onDismiss()
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0f) accumulatedDrag += dragAmount
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row: word, POS, star toggle, close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = word.word,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (word.partOfSpeech.isNotBlank()) {
                            Text(
                                text = "(${word.partOfSpeech})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (word.phonetic.isNotBlank()) {
                        Text(
                            text = word.phonetic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "${VocabularyDecks.shortLabelFor(word.deckId)} • ${word.topic}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassIconButton(
                        onClick = { onToggleStarred(word) },
                        size = 40.dp,
                        containerColor = if (word.isStarred) {
                            Color(0xFFF59E0B).copy(alpha = if (glassColors.isDark) 0.20f else 0.16f)
                        } else if (glassColors.isDark) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            Color.White.copy(alpha = 0.65f)
                        },
                        contentColor = starColor
                    ) {
                        Icon(
                            imageVector = if (word.isStarred) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = if (word.isStarred) {
                                "Bỏ đánh dấu sao ${word.word}"
                            } else {
                                "Đánh dấu sao ${word.word}"
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    LiquidGlassIconButton(
                        onClick = onDismiss,
                        size = 40.dp
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Đóng chi tiết từ",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!isEditing) {
                // Pronunciation controls: 1.0x and 0.75x
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassButton(
                        onClick = { onSpeakAtRate(word.word, 1.0f) },
                        tintColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (glassColors.isDark) 0.22f else 0.16f
                        ),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Phát âm 1.0x", style = MaterialTheme.typography.labelLarge)
                    }

                    LiquidGlassButton(
                        onClick = { onSpeakAtRate(word.word, 0.75f) },
                        tintColor = if (glassColors.isDark) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            Color.White.copy(alpha = 0.65f)
                        },
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SlowMotionVideo,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chậm 0.75x", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Meaning & Bilingual Example card
                LiquidGlassCard(
                    shape = DetailCardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Nghĩa tiếng Việt",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = word.meaningVi,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (word.exampleEn.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (glassColors.isDark) Color.White.copy(alpha = 0.05f)
                                        else Color.White.copy(alpha = 0.50f)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = word.exampleEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (word.exampleVi.isNotBlank()) {
                                        Text(
                                            text = word.exampleVi,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Learner-friendly Progress card
                val nextReviewNote = if (word.intervalDays > 0) {
                    "Ôn lại sau ${word.intervalDays} ngày"
                } else {
                    "Sẵn sàng học ngay"
                }
                LiquidGlassCard(
                    shape = DetailCardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Tiến độ ghi nhớ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Trạng thái: $statusLabel • $nextReviewNote",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(statusBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusFg
                            )
                        }
                    }
                }

                // Custom user word edit/delete actions
                if (isCustomWord) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassButton(
                            onClick = { isEditing = true },
                            tintColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (glassColors.isDark) 0.22f else 0.16f
                            ),
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chỉnh sửa", style = MaterialTheme.typography.labelLarge)
                        }

                        LiquidGlassButton(
                            onClick = { showDeleteConfirm = true },
                            tintColor = MaterialTheme.colorScheme.error.copy(
                                alpha = if (glassColors.isDark) 0.22f else 0.14f
                            ),
                            contentColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xóa từ", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // Secondary low-emphasis reset action at bottom
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = {
                            onResetProgress(word)
                            onShowMessage("Đã đặt lại tiến độ SM-2 cho \"${word.word}\"")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đặt lại tiến độ SM-2",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Inline Edit Form (only reachable for custom_user words)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailEditField(
                        label = "Từ tiếng Anh *",
                        value = editWord,
                        onValueChange = { editWord = it }
                    )
                    DetailEditField(
                        label = "Phiên âm IPA",
                        value = editPhonetic,
                        onValueChange = { editPhonetic = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DetailPosChips.forEach { pos ->
                            val selected = editPos == pos
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else if (glassColors.isDark) Color.White.copy(alpha = 0.08f)
                                        else Color.White.copy(alpha = 0.62f)
                                    )
                                    .clickable { editPos = pos }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pos,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    DetailEditField(
                        label = "Nghĩa tiếng Việt *",
                        value = editMeaningVi,
                        onValueChange = { editMeaningVi = it }
                    )
                    DetailEditField(
                        label = "Chủ đề",
                        value = editTopic,
                        onValueChange = { editTopic = it }
                    )
                    DetailEditField(
                        label = "Ví dụ tiếng Anh",
                        value = editExampleEn,
                        onValueChange = { editExampleEn = it },
                        singleLine = false
                    )
                    DetailEditField(
                        label = "Dịch ví dụ tiếng Việt",
                        value = editExampleVi,
                        onValueChange = { editExampleVi = it },
                        singleLine = false
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassButton(
                            onClick = { isEditing = false },
                            enabled = !isSaving,
                            tintColor = if (glassColors.isDark) {
                                Color.White.copy(alpha = 0.08f)
                            } else {
                                Color.White.copy(alpha = 0.65f)
                            },
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hủy", style = MaterialTheme.typography.labelLarge)
                        }

                        LiquidGlassButton(
                            onClick = {
                                if (editWord.isBlank() || editMeaningVi.isBlank()) {
                                    onShowMessage("Vui lòng nhập từ tiếng Anh và nghĩa tiếng Việt")
                                    return@LiquidGlassButton
                                }
                                isSaving = true
                                coroutineScope.launch {
                                    val updated = onUpdateCustomWord(
                                        word.copy(
                                            word = editWord.trim(),
                                            phonetic = editPhonetic.trim(),
                                            partOfSpeech = editPos.trim().ifEmpty { "n." },
                                            meaningVi = editMeaningVi.trim(),
                                            topic = editTopic.trim().ifEmpty { "Chung" },
                                            exampleEn = editExampleEn.trim(),
                                            exampleVi = editExampleVi.trim()
                                        )
                                    )
                                    isSaving = false
                                    if (updated) {
                                        isEditing = false
                                        onShowMessage("Đã cập nhật từ \"${editWord.trim()}\"")
                                    } else {
                                        onShowMessage("Không thể cập nhật từ này")
                                    }
                                }
                            },
                            enabled = !isSaving,
                            tintColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isSaving) "Đang lưu..." else "Lưu thay đổi",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true
) {
    val glassColors = LocalLiquidGlassColors.current
    val fill = if (glassColors.isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.65f)
    val borderBrush = rememberLiquidGlassBorderBrush(glassColors = glassColors)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(DetailInputShape)
                .background(fill)
                .border(width = 0.8.dp, brush = borderBrush, shape = DetailInputShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
