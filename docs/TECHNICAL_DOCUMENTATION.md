# Tài Liệu Kỹ Thuật — Lexora (`v1.4.0`)

Tài liệu này mô tả kiến trúc phần mềm, thuật toán SM-2, hệ thống giao diện thích ứng đa thiết bị (Phone / Foldable / Tablet), đồ họa Kính mờ tối giản (Minimalist Glassmorphism & Flat Surface) 120Hz ARR, lưu trữ Room v4, bảo mật chữ ký số và quy trình CI/CD của **Lexora** (*TOEIC Vocabulary • SM-2*).

---

## 1. Kiến Trúc Tổng Thể & Manual DI

Ứng dụng áp dụng **Clean Architecture 3 tầng** kết hợp **MVVM** và **Manual Dependency Injection** qua [`AppContainer.kt`](../app/src/main/java/com/toeicflashcard/di/AppContainer.kt), tách biệt toàn bộ logic nghiệp vụ Kotlin thuần (`domain/`) khỏi Android Framework.

- **Tầng `domain/` (Pure Kotlin):**
  - Models bất biến: `VocabularyWord`, `VocabularyDecks`, `HomeSummary`, `TopicProgress`, `UserPreferences`, `DarkTone`, `QuizModels`, `ReviewQuality`, `Sm2Result`.
  - Engines: `Sm2Scheduler`, `StreakCalculator`.
  - 6 Use Cases: `ObserveHomeSummaryUseCase`, `GetStudyQueueUseCase`, `SubmitReviewUseCase`, `GenerateQuizUseCase`, `MarkWordsUrgentReviewUseCase`, `AddCustomVocabularyUseCase`.
- **Tầng `data/`:**
  - `ToeicDatabase` (`version = 4`, bổ sung `isStarred` + migration v3→v4), `VocabularyDao`, `VocabularySeeder` (`SEED_VERSION = 3` bảo vệ bởi `Mutex`).
  - `VocabularyRepositoryImpl` (kết hợp in-memory cache để mở `StudyScreen` / `QuizScreen` với `0ms` loading flash).
  - `UserPreferencesDataStore` (`13` khóa Preferences + `hasCompletedOnboarding` + preview độ đục kính thời gian thực trong bộ nhớ).
  - `DailyReminderWorker` (`WorkManager` chu kỳ 24 giờ) và `ReminderBootReceiver` (`BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`).
- **Tầng `presentation/`:**
  - `ToeicNavHost` giữ ấm `MainTabShell` khi đẩy các overlay routes `DeckTopicsRoute`, `StudyRoute` và `QuizRoute`.
  - `MainTabShell` quản lý `HazeSource` dùng chung, `CollapsibleHeaderState` (hỗ trợ ngưỡng cuộn tùy chỉnh `collapseThresholdPx`, `expandThresholdPx = 52.dp` trên `DeckTopicsScreen`), `LiquidGlassBottomDock` và các in-window bottom sheets/dialogs (`WordDetailSheet`, `OnboardingSheet`, `AddVocabularySheet`, `ReminderSettingsSheet`).
  - **Kiểm thử tự động:** 19 bộ Unit Test JVM (`Phase1CoreLearningTest`, `Phase2VaultAndSyncTest`, `Phase3TtsAndReminderTest`, `Phase4OnboardingTest`, `ResponsiveAndIconJvmRenderTest`, `SystemFlowAndAdaptiveUiTest`, v.v.).

---

## 2. Đặc Tả Thuật Toán SM-2 & Mục Tiêu Ngày (`v1.3.0`)

### 2.1. Tách bạch Từ đến hạn ôn vs Từ mới chưa học
Trong [`ObserveHomeSummaryUseCase.kt`](../app/src/main/java/com/toeicflashcard/domain/usecase/ObserveHomeSummaryUseCase.kt) và [`HomeUiState.kt`](../app/src/main/java/com/toeicflashcard/presentation/home/HomeUiState.kt):
- **`reviewDueCount`**: Chỉ đếm các từ **đã từng học** (`repetitions > 0`) và đã đến hạn ôn lại (`nextReviewTimestamp <= endOfTodayMillis`).
- **`newWordsCount`**: Chỉ đếm các từ **mới tinh chưa học** (`repetitions == 0 && !isLearned`).
- **`continueTopic`**: Tự động tìm chủ đề đang học dở (`0 < learnedCount < totalCount`) có `lastReviewedTimestamp` gần nhất để hiển thị lối tắt `Tiếp tục: [Chủ đề] (X/Y)` ngay trong Hero Card.

### 2.2. Công thức SuperMemo-2 (`Sm2Scheduler.kt`)
Với điểm chất lượng $q \in \{1, 3, 4, 5\}$ (`AGAIN = 1`, `HARD = 3`, `GOOD = 4`, `EASY = 5`):

$$EF' = \max\!\left(1.3,\; EF + 0.1 - (5 - q)\bigl(0.08 + 0.02(5 - q)\bigr)\right)$$

$$I(n) = \begin{cases} 1 \text{ (đẩy về cuối hàng đợi phiên hiện tại)}, & \text{nếu } q < 3 \\ 1 \text{ ngày}, & \text{nếu } q \ge 3 \text{ và } n = 0 \\ 6 \text{ ngày}, & \text{nếu } q \ge 3 \text{ và } n = 1 \\ \operatorname{round}(I(n-1) \times EF') \text{ ngày}, & \text{nếu } q \ge 3 \text{ và } n \ge 2 \end{cases}$$

---

## 3. Hệ Thống Giao Diện Thích Ứng 2 Trục (`AppWindowMetrics` & `AdaptiveLayout`)

Ứng dụng đo kích thước cửa sổ thực tế từ `BoxWithConstraints` gốc trong `MainActivity` và cung cấp xuống toàn bộ cây Compose qua `LocalAppWindowMetrics`:

- **`AppWindowMetrics`** ([`AppWindowMetrics.kt`](../app/src/main/java/com/toeicflashcard/core/ui/glass/AppWindowMetrics.kt)):
  - `isExpandedWidth`: `widthDp >= 700.dp` (Foldable mở, Tablet, hoặc điện thoại xoay ngang).
  - `isLandscapeShort`: `widthDp >= 680.dp && heightDp < 650.dp` (bật bố cục 2 khung ngang cho `StudyScreen` và `QuizScreen`).
- **Khoảng thở động iPadOS-style** ([`AdaptiveLayout.kt`](../app/src/main/java/com/toeicflashcard/core/ui/glass/AdaptiveLayout.kt)):

| Token | Điện thoại (`< 600.dp`) | Fold / Tablet dọc (`600–999.dp`) | Tablet ngang (`>= 1000.dp`) |
| --- | :---: | :---: | :---: |
| `screenHorizontalPadding` | `16.dp` | `28.dp` | `40.dp` + căn giữa `MainContentMaxWidth` (`1100.dp`) |
| `sectionVerticalSpacing` | `14.dp` | `22.dp` | `22.dp` |
| `gridItemSpacing` | `10.dp` | `16.dp` | `16.dp` |
| `cardInnerPadding` | `16.dp` | `22.dp` | `22.dp` |
| `Study` / `Quiz` MaxWidth | `560.dp` / `580.dp` | `720.dp` / `700.dp` | `720.dp` / `700.dp` |

### Ma trận kiểm thử 11 cấu hình thiết bị (`Q1`–`Q11` trong `tools/launch_toeic_avd.ps1`)

| Mã | Thiết bị / Trạng thái | Độ phân giải & DPI | Chiều rộng (`dp`) | Số cột chủ đề | Chế độ Study/Quiz |
| --- | --- | --- | :---: | :---: | --- |
| `Q1` | TECNO CLA5 (Dọc 20.3:9) | `1080×2436 @ 420dpi` | `411.4 dp` | `1` | 1 cột dọc |
| `Q2` | Điện thoại xoay ngang | `2436×1080 @ 420dpi` | `928.0 dp` | `2` | 2-pane ngang (`isLandscapeShort`) |
| `Q3` | Galaxy Z Fold 7 Cover (22:9) | `968×2376 @ 420dpi` | `368.8 dp` | `1` | 1 cột dọc |
| `Q4` | Galaxy Z Fold 7 Inner (20:18) | `1968×2184 @ 374dpi` | `841.9 dp` | `2` | Rộng `720.dp` / `700.dp` |
| `Q5` | OPPO Pad SE (7:5 Dọc) | `1600×2240 @ 280dpi` | `914.3 dp` | `2` | Rộng `720.dp` / `700.dp` |
| `Q6` | Redmi Pad 2 (16:10 Dọc) | `1600×2560 @ 274dpi` | `934.3 dp` | `2` | Rộng `720.dp` / `700.dp` |
| `Q7` | Redmi Pad 2 (16:10 Ngang) | `2560×1600 @ 274dpi` | `1494.9 dp` | `3` | Rộng `720.dp` / `700.dp` |
| `Q8` | Xiaomi Pad 8 (3:2 Dọc) | `2136×3200 @ 345dpi` | `990.6 dp` | `2` | Rộng `720.dp` / `700.dp` |
| `Q9` | Split-Screen chia đôi | `1080×1218 @ 420dpi` | `411.4 dp` | `1` | 1 cột dọc |
| `Q10` | Galaxy Z Fold 8 Cover (5.5") | `1248×1972 @ 420dpi` | `475.4 dp` | `1` | 1 cột dọc |
| `Q11` | Galaxy Z Fold 8 Inner (7.6") | `1848×2448 @ 370dpi` | `799.1 dp` | `2` | Rộng `720.dp` / `700.dp` |

---

## 4. Đồ Họa Kính Mờ Tối Giản (Frosted Glass) & Hiệu Năng 120Hz ARR

- **Kính mờ thời gian thực (`dev.chrisbanes.haze:haze:1.0.0`):** Dùng 1 `HazeState` nguồn duy nhất tại `MainTabShell` áp dụng cho Floating Header, Search Pill, Bottom Dock và In-window Sheets/Dialogs.
- **Draw-phase Animations:** `CollapsibleHeaderState` và `LiquidGlassBottomDock` biến đổi `translationY`, `alpha`, `scaleX`, `scaleY` trực tiếp trong `graphicsLayer` -> **0 recomposition** khi cuộn.
- **120Hz Adaptive Refresh Rate (`MainActivity.kt`):** Chọn `Display.Mode` có tần số cao nhất khớp độ phân giải màn hình hiện tại (ví dụ `120Hz`), kết hợp `isFrameRatePowerSavingsBalanced = true` và `frameRateBoostOnTouchEnabled = true` trên Android 15+ (API 35+).

---

## 5. Lưu Trữ Room v3, Bảo Mật Ký Số & CI/CD

### 5.1. Cơ sở dữ liệu & Migration (`ToeicDatabase` v3)
- Bảng `vocabulary_words` đánh index trên `topic`, `nextReviewTimestamp` và `deckId`.
- `MIGRATION_1_2`: Thêm index `topic` và `nextReviewTimestamp`.
- `MIGRATION_2_3`: Thêm cột `deckId` và `deckName`, giữ nguyên **100% lịch sử SM-2** của người dùng khi nâng cấp từ 775 từ lên 2.154+ từ.
- **0-Hardcode Dynamic Counts:** Toàn bộ con số hiển thị trên Header, Search placeholder, Vault, Practice và Stats đều truy vấn trực tiếp từ Room (`Flow<List<VocabularyWord>>`) và định dạng bằng `NumberFormat` (`vi-VN`).

### 5.2. Bảo mật Ký số & R8 ProGuard
- Keystore cục bộ (`keystore/toeic_release.jks`) và `keystore.properties` được loại trừ tuyệt đối trong [`.gitignore`](../.gitignore).
- Bản `release` bật `isMinifyEnabled = true` + `isShrinkResources = true` với luật giữ an toàn `@Serializable`, Navigation Routes, Room Entities/DAOs và `DailyReminderWorker` tại [`proguard-rules.pro`](../app/proguard-rules.pro).

### 5.3. Quy trình CI/CD (`.github/workflows/android-release.yml`)
- Tự động chạy khi push tag `v*` hoặc kích hoạt thủ công (`workflow_dispatch`).
- Sử dụng `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true` và `actions/setup-java@v5` (JDK 17).
- Chạy `./gradlew testDebugUnitTest --no-daemon` và `./gradlew assembleRelease --no-daemon`. Nếu 4 Repository Secrets ký số đã được cấu hình trên GitHub, workflow tự động ký APK và phát hành GitHub Release kèm `checksums-sha256.txt`.
