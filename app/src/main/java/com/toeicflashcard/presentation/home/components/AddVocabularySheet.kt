package com.toeicflashcard.presentation.home.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toeicflashcard.core.ui.glass.FocusedContentMaxWidth
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.glass.LiquidGlassSheetSurface
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.usecase.AddCustomVocabularyUseCase
import com.toeicflashcard.domain.usecase.AddWordResult
import com.toeicflashcard.domain.usecase.CustomWordInput
import com.toeicflashcard.domain.usecase.ImportJsonResult
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AddVocabTab(val label: String) {
    MANUAL("Nhập từ mới"),
    JSON_IMPORT("Nhập file JSON")
}

private val PartOfSpeechChips = listOf("n.", "v.", "adj.", "adv.", "phr.")
private val AddSheetCardShape = RoundedCornerShape(20.dp)
private val InputFieldShape = RoundedCornerShape(14.dp)

@Composable
fun AddVocabularySheet(
    visible: Boolean,
    hazeState: HazeState,
    existingTopics: List<String>,
    onAddWord: suspend (CustomWordInput) -> AddWordResult,
    onImportJson: suspend (String) -> ImportJsonResult,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current

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
                    .semantics { contentDescription = "Đóng thêm từ vựng" }
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
            AddVocabularySheetContent(
                hazeState = hazeState,
                existingTopics = existingTopics,
                onAddWord = onAddWord,
                onImportJson = onImportJson,
                onShowMessage = onShowMessage,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun AddVocabularySheetContent(
    hazeState: HazeState,
    existingTopics: List<String>,
    onAddWord: suspend (CustomWordInput) -> AddWordResult,
    onImportJson: suspend (String) -> ImportJsonResult,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    var selectedTab by remember { mutableStateOf(AddVocabTab.MANUAL) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    val pillTrackColor = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    }
    val accentTint = MaterialTheme.colorScheme.primary

    LiquidGlassSheetSurface(
        hazeState = hazeState,
        blurRadius = 20.dp,
        sheetMaxWidth = FocusedContentMaxWidth,
        modifier = Modifier
            .widthIn(max = FocusedContentMaxWidth)
            .fillMaxWidth()
            .imePadding()
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryAdd,
                        contentDescription = null,
                        tint = accentTint
                    )
                    Text(
                        text = "Thêm từ vựng mới",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                LiquidGlassIconButton(
                    onClick = onDismiss,
                    size = 36.dp
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Đóng",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "add_vocab_tab_transition"
            ) { tab ->
                when (tab) {
                    AddVocabTab.MANUAL -> ManualAddWordForm(
                        existingTopics = existingTopics,
                        onAddWord = onAddWord,
                        onShowMessage = onShowMessage,
                        onDismiss = onDismiss
                    )

                    AddVocabTab.JSON_IMPORT -> JsonImportForm(
                        onImportJson = onImportJson,
                        onShowMessage = onShowMessage,
                        onDismiss = onDismiss
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        selectedTab = if (selectedTab == AddVocabTab.MANUAL) {
                            AddVocabTab.JSON_IMPORT
                        } else {
                            AddVocabTab.MANUAL
                        }
                    }
                ) {
                    Text(
                        text = if (selectedTab == AddVocabTab.MANUAL) {
                            "Nhập hàng loạt từ file JSON ›"
                        } else {
                            "‹ Quay lại nhập từng từ"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualAddWordForm(
    existingTopics: List<String>,
    onAddWord: suspend (CustomWordInput) -> AddWordResult,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val coroutineScope = rememberCoroutineScope()

    var word by remember { mutableStateOf("") }
    var phonetic by remember { mutableStateOf("") }
    var partOfSpeech by remember { mutableStateOf("n.") }
    var meaningVi by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf(AddCustomVocabularyUseCase.DEFAULT_CUSTOM_TOPIC) }
    var topicDropdownExpanded by remember { mutableStateOf(false) }
    var isCustomTopicInput by remember { mutableStateOf(false) }
    var showOptionalFields by remember { mutableStateOf(false) }
    var exampleEn by remember { mutableStateOf("") }
    var exampleVi by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val topicSuggestions = remember(existingTopics) {
        buildList {
            add(AddCustomVocabularyUseCase.DEFAULT_CUSTOM_TOPIC)
            existingTopics.forEach { t ->
                if (t != AddCustomVocabularyUseCase.DEFAULT_CUSTOM_TOPIC) {
                    add(t)
                }
            }
        }
    }
    val topicPillFill = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.68f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Core fields
        GlassTextInputField(
            label = "Từ tiếng Anh",
            value = word,
            onValueChange = {
                word = it
                if (errorMessage != null) errorMessage = null
            },
            placeholder = "Nhập từ vựng...",
            imeAction = ImeAction.Next
        )

        GlassTextInputField(
            label = "Nghĩa tiếng Việt",
            value = meaningVi,
            onValueChange = {
                meaningVi = it
                if (errorMessage != null) errorMessage = null
            },
            placeholder = "Nhập nghĩa tiếng Việt...",
            imeAction = ImeAction.Done
        )

        // 2. Part of speech & Topic
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Loại từ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PartOfSpeechChips.forEach { pos ->
                        FrostedSelectionChip(
                            label = pos,
                            selected = partOfSpeech == pos,
                            onClick = { partOfSpeech = pos },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Chủ đề",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(topicPillFill)
                            .border(
                                width = if (glassColors.isDark) 0.75.dp else 1.dp,
                                brush = glassColors.defaultBorderBrush,
                                shape = CircleShape
                            )
                            .clickable { topicDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isCustomTopicInput && topic.isBlank()) {
                                "Chủ đề mới..."
                            } else {
                                topic.ifBlank { AddCustomVocabularyUseCase.DEFAULT_CUSTOM_TOPIC }
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Chọn chủ đề",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = topicDropdownExpanded,
                        onDismissRequest = { topicDropdownExpanded = false },
                        modifier = Modifier.heightIn(max = 260.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "+ Nhập chủ đề mới...",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                topicDropdownExpanded = false
                                isCustomTopicInput = true
                                if (topic == AddCustomVocabularyUseCase.DEFAULT_CUSTOM_TOPIC) {
                                    topic = ""
                                }
                            }
                        )
                        topicSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = suggestion,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    topicDropdownExpanded = false
                                    isCustomTopicInput = false
                                    topic = suggestion
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isCustomTopicInput,
                enter = fadeIn(tween(160)) + expandVertically(tween(200)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
            ) {
                GlassTextInputField(
                    label = "Tên chủ đề mới",
                    value = topic,
                    onValueChange = { topic = it },
                    placeholder = "Ví dụ: Hợp đồng, Thuế...",
                    imeAction = ImeAction.Done
                )
            }
        }

        // 3. Progressive Disclosure for IPA & Examples
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { showOptionalFields = !showOptionalFields }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (showOptionalFields) {
                    "Ẩn phiên âm IPA & Câu ví dụ"
                } else {
                    "+ Thêm phiên âm IPA & Câu ví dụ"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            if (showOptionalFields) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showOptionalFields,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextInputField(
                    label = "Phiên âm IPA",
                    value = phonetic,
                    onValueChange = { phonetic = it },
                    placeholder = "/ˈmaɪlstoʊn/",
                    imeAction = ImeAction.Next
                )

                GlassTextInputField(
                    label = "Ví dụ tiếng Anh",
                    value = exampleEn,
                    onValueChange = { exampleEn = it },
                    placeholder = "Nhập câu ví dụ...",
                    singleLine = false,
                    imeAction = ImeAction.Next
                )

                GlassTextInputField(
                    label = "Dịch ví dụ",
                    value = exampleVi,
                    onValueChange = { exampleVi = it },
                    placeholder = "Nhập dịch nghĩa...",
                    singleLine = false,
                    imeAction = ImeAction.Done
                )
            }
        }

        if (errorMessage != null) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                borderBrush = rememberLiquidGlassBorderBrush(
                    glassColors = glassColors,
                    accentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        LiquidGlassButton(
            onClick = {
                if (isSaving) return@LiquidGlassButton
                isSaving = true
                errorMessage = null
                coroutineScope.launch {
                    val result = onAddWord(
                        CustomWordInput(
                            word = word,
                            phonetic = phonetic,
                            partOfSpeech = partOfSpeech,
                            meaningVi = meaningVi,
                            topic = topic.ifBlank { AddCustomVocabularyUseCase.DEFAULT_CUSTOM_TOPIC },
                            exampleEn = exampleEn,
                            exampleVi = exampleVi
                        )
                    )
                    isSaving = false
                    when (result) {
                        is AddWordResult.Success -> {
                            onShowMessage(
                                "Đã thêm \"${result.addedWord.word}\" (#${result.addedWord.id}) vào \"${result.addedWord.topic}\""
                            )
                            onDismiss()
                        }

                        is AddWordResult.DuplicateWord -> {
                            errorMessage = "Từ \"${result.existingWord}\" đã tồn tại trong kho từ."
                        }

                        is AddWordResult.ValidationError -> {
                            errorMessage = result.messageVi
                        }
                    }
                }
            },
            enabled = !isSaving,
            tintColor = if (glassColors.isDark) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            },
            contentColor = if (glassColors.isDark) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            borderColor = if (glassColors.isDark) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isSaving) "Đang lưu..." else "Lưu & Đưa vào ôn tập",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun JsonImportForm(
    onImportJson: suspend (String) -> ImportJsonResult,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val glassColors = LocalLiquidGlassColors.current
    val coroutineScope = rememberCoroutineScope()

    var isImporting by remember { mutableStateOf(false) }
    var importFeedback by remember { mutableStateOf<String?>(null) }
    var isErrorFeedback by remember { mutableStateOf(false) }

    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImporting = true
        importFeedback = null
        coroutineScope.launch {
            val rawText = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (rawText == null) {
                isImporting = false
                isErrorFeedback = true
                importFeedback = "Không thể đọc nội dung file đã chọn."
                return@launch
            }
            val result = onImportJson(rawText)
            isImporting = false
            if (result.errorMessageVi != null) {
                isErrorFeedback = true
                importFeedback = result.errorMessageVi
            } else {
                val summary = buildImportSummaryMessage(result)
                if (result.addedCount > 0) {
                    onShowMessage(summary)
                    onDismiss()
                } else {
                    isErrorFeedback = false
                    importFeedback = summary
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Định dạng JSON mẫu (bắt buộc word và meaningVi):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = AddSheetCardShape,
            containerColor = glassColors.surfaceFill
        ) {
            Text(
                text = AddCustomVocabularyUseCase.SAMPLE_JSON_TEMPLATE,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(14.dp)
            )
        }

        if (importFeedback != null) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                containerColor = if (isErrorFeedback) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                } else {
                    glassColors.surfaceFill
                },
                borderBrush = rememberLiquidGlassBorderBrush(
                    glassColors = glassColors,
                    accentColor = if (isErrorFeedback) MaterialTheme.colorScheme.error else null
                )
            ) {
                Text(
                    text = importFeedback.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isErrorFeedback) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        LiquidGlassButton(
            onClick = {
                clipboard.setText(AnnotatedString(AddCustomVocabularyUseCase.SAMPLE_JSON_TEMPLATE))
                onShowMessage("Đã sao chép mẫu JSON vào bộ nhớ tạm")
            },
            tintColor = if (glassColors.isDark) {
                Color.White.copy(alpha = 0.09f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            borderColor = if (glassColors.isDark) null else MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sao chép mẫu JSON",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        LiquidGlassButton(
            onClick = {
                if (!isImporting) {
                    openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            },
            enabled = !isImporting,
            tintColor = if (glassColors.isDark) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            },
            contentColor = if (glassColors.isDark) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            borderColor = if (glassColors.isDark) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isImporting) "Đang nạp dữ liệu..." else "Chọn file .json từ thiết bị",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GlassTextInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next
) {
    val glassColors = LocalLiquidGlassColors.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 44.dp else 64.dp)
                .clip(InputFieldShape)
                .background(glassColors.surfaceFill)
                .border(
                    width = if (glassColors.isDark) 0.75.dp else 1.dp,
                    brush = glassColors.defaultBorderBrush,
                    shape = InputFieldShape
                )
                .padding(horizontal = 14.dp, vertical = 11.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    maxLines = if (singleLine) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else 3,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = imeAction)
            )
        }
    }
}

@Composable
private fun FrostedSelectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val bg = when {
        selected && glassColors.isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        glassColors.isDark -> Color.White.copy(alpha = 0.06f)
        else -> Color.White.copy(alpha = 0.62f)
    }
    val fg = when {
        selected && glassColors.isDark -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .border(
                width = if (selected) 1.dp else 0.75.dp,
                brush = glassColors.defaultBorderBrush,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildImportSummaryMessage(result: ImportJsonResult): String {
    return buildString {
        append("Đã thêm ${result.addedCount} từ mới")
        val skips = mutableListOf<String>()
        if (result.skippedDuplicateCount > 0) {
            skips += "bỏ qua ${result.skippedDuplicateCount} từ trùng"
        }
        if (result.skippedInvalidCount > 0) {
            skips += "${result.skippedInvalidCount} mục thiếu thông tin"
        }
        if (skips.isNotEmpty()) {
            append(" (${skips.joinToString(", ")})")
        }
    }
}
