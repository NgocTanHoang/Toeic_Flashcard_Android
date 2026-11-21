package com.toeicflashcard.presentation.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.toeicflashcard.core.ui.glass.AppWindowMetrics
import com.toeicflashcard.core.ui.glass.LocalAppWindowMetrics
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.presentation.home.HomeViewModel
import com.toeicflashcard.presentation.main.MainTabShell
import com.toeicflashcard.presentation.quiz.QuizScreen
import com.toeicflashcard.presentation.quiz.QuizViewModel
import com.toeicflashcard.presentation.study.StudyScreen
import com.toeicflashcard.presentation.study.StudyViewModel
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class StudyRoute(
    val topic: String? = null,
    val hardWordsOnly: Boolean = false,
    val deckId: String? = null
)

@Serializable
data class QuizRoute(
    val topic: String? = null,
    val listeningMode: Boolean = false,
    val deckId: String? = null
)

private val NavPushEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private const val NAV_ENTER_DURATION_MS = 280
private const val NAV_POP_DURATION_MS = 260

@Composable
fun ToeicNavHost() {
    val navController = rememberNavController()
    val glassColors = LocalLiquidGlassColors.current
    val focusManager = LocalFocusManager.current
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    var isOverlaySettled by remember { mutableStateOf(false) }

    val isOnHome = currentBackStackEntry == null ||
        currentBackStackEntry?.destination?.hasRoute<HomeRoute>() == true
    val isRootVisible = isOnHome || !isOverlaySettled

    fun NavBackStackEntry.canNavigate(): Boolean =
        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    fun navigateToOverlay(route: Any) {
        val current = navController.currentBackStackEntry ?: return
        if (current.destination.hasRoute<HomeRoute>() && current.canNavigate()) {
            focusManager.clearFocus(force = true)
            isOverlaySettled = false
            navController.navigate(route)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(glassColors.hazeBackground)
    ) {
        val windowMetrics = remember(maxWidth, maxHeight) {
            AppWindowMetrics(widthDp = maxWidth, heightDp = maxHeight)
        }
        CompositionLocalProvider(LocalAppWindowMetrics provides windowMetrics) {
        // Keep MainTabShell warm in composition so Back returns with 0ms re-composition delay;
        // skip draw and clear semantics only while a pushed Study/Quiz sheet completely covers it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    if (isRootVisible) {
                        drawContent()
                    }
                }
                .then(
                    if (!isRootVisible) {
                        Modifier.clearAndSetSemantics {}
                    } else {
                        Modifier
                    }
                )
        ) {
            MainTabShell(
                homeViewModel = homeViewModel,
                onStartDueReview = {
                    val activeDeckId = homeViewModel.uiState.value.selectedDeckId
                    navigateToOverlay(
                        StudyRoute(
                            topic = null,
                            hardWordsOnly = false,
                            deckId = activeDeckId
                        )
                    )
                },
                onSelectTopic = { topic ->
                    val activeDeckId = homeViewModel.uiState.value.selectedDeckId
                    navigateToOverlay(
                        StudyRoute(
                            topic = topic,
                            hardWordsOnly = false,
                            deckId = activeDeckId
                        )
                    )
                },
                onReviewHardWords = {
                    navigateToOverlay(StudyRoute(topic = null, hardWordsOnly = true, deckId = null))
                },
                onStartQuiz = { topic, listeningMode ->
                    val activeDeckId = homeViewModel.uiState.value.selectedDeckId
                    navigateToOverlay(
                        QuizRoute(
                            topic = topic,
                            listeningMode = listeningMode,
                            deckId = activeDeckId
                        )
                    )
                }
            )
        }

        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(durationMillis = NAV_ENTER_DURATION_MS, easing = NavPushEasing)
                ) { fullWidth -> fullWidth }
            },
            exitTransition = {
                ExitTransition.None
            },
            popEnterTransition = {
                EnterTransition.None
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = NAV_POP_DURATION_MS, easing = NavPushEasing)
                ) { fullWidth -> fullWidth }
            }
        ) {
            composable<HomeRoute> {}

            composable<StudyRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<StudyRoute>()
                val studyViewModel: StudyViewModel = viewModel(
                    factory = StudyViewModel.provideFactory(
                        topic = route.topic,
                        hardWordsOnly = route.hardWordsOnly,
                        deckId = route.deckId
                    )
                )
                OverlayRouteHost(
                    isDark = glassColors.isDark,
                    onSettledChanged = { settled -> isOverlaySettled = settled }
                ) {
                    StudyScreen(
                        viewModel = studyViewModel,
                        onNavigateBack = {
                            if (backStackEntry.canNavigate()) {
                                isOverlaySettled = false
                                navController.popBackStack()
                            }
                        }
                    )
                }
            }

            composable<QuizRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<QuizRoute>()
                val quizViewModel: QuizViewModel = viewModel(
                    factory = QuizViewModel.provideFactory(
                        topic = route.topic,
                        isListeningMode = route.listeningMode,
                        deckId = route.deckId
                    )
                )
                OverlayRouteHost(
                    isDark = glassColors.isDark,
                    onSettledChanged = { settled -> isOverlaySettled = settled }
                ) {
                    QuizScreen(
                        viewModel = quizViewModel,
                        onNavigateBack = {
                            if (backStackEntry.canNavigate()) {
                                isOverlaySettled = false
                                navController.popBackStack()
                            }
                        }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun AnimatedContentScope.OverlayRouteHost(
    isDark: Boolean,
    onSettledChanged: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    LaunchedEffect(transition.currentState, transition.targetState) {
        onSettledChanged(
            transition.currentState == EnterExitState.Visible &&
                transition.targetState == EnterExitState.Visible
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            onSettledChanged(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Main).changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        content()
        if (transition.isRunning) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.14f)
                        else Color.Black.copy(alpha = 0.08f)
                    )
            )
        }
    }
}
