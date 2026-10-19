package com.example.personalfitnessapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.personalfitnessapp.ui.browser.ExerciseBrowserScreen
import com.example.personalfitnessapp.ui.coach.CoachChatScreen
import com.example.personalfitnessapp.ui.detail.ExerciseDetailScreen
import com.example.personalfitnessapp.ui.detail.ExerciseHistoryScreen
import com.example.personalfitnessapp.ui.history.SessionDetailScreen
import com.example.personalfitnessapp.ui.history.SessionHistoryScreen
import com.example.personalfitnessapp.ui.home.HomeScreen
import com.example.personalfitnessapp.ui.metrics.BodyMetricsScreen
import com.example.personalfitnessapp.ui.onboarding.OnboardingScreen
import com.example.personalfitnessapp.ui.photo.PhotoCaptureScreen
import com.example.personalfitnessapp.ui.photo.PhotoConfirmationScreen
import com.example.personalfitnessapp.ui.profile.ProfileScreen
import com.example.personalfitnessapp.ui.recovery.RecoveryScreen
import com.example.personalfitnessapp.ui.routines.RoutineEditorScreen
import com.example.personalfitnessapp.ui.routines.RoutinesScreen
import com.example.personalfitnessapp.ui.routines.WeeklyScheduleScreen
import com.example.personalfitnessapp.ui.session.ExercisePickerScreen
import com.example.personalfitnessapp.ui.session.WorkoutSessionScreen
import com.example.personalfitnessapp.ui.settings.SettingsScreen
import com.example.personalfitnessapp.ui.swap.SwapScreen
import com.example.personalfitnessapp.ui.today.TodayScreen

/**
 * Bottom nav is 3 tabs (Home / Workout / Profile) per docs/00_overview.md and
 * docs/01_design_system.md, not the 6-tab bar this app had before the
 * Hevy-UX pass. WORKOUT_TAB reuses the existing "Today" screen (start/resume
 * a workout, today's routine, recovery warning) since that's functionally
 * docs/screens/workout_tab.md's content; ROUTINES, SESSION_HISTORY,
 * BODY_METRICS, RECOVERY, COACH, EXERCISE_BROWSER move behind the Profile
 * hub instead of being top-level tabs.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val WORKOUT_TAB = "workout_tab"
    const val PROFILE = "profile"
    const val EXERCISE_BROWSER = "exercise_browser"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val EXERCISE_HISTORY = "exercise_history/{exerciseId}"
    const val SETTINGS = "settings"
    const val WORKOUT_SESSION = "workout_session?routineId={routineId}"
    const val EXERCISE_PICKER = "exercise_picker"
    const val SESSION_HISTORY = "session_history"
    const val BODY_METRICS = "body_metrics"
    const val COACH = "coach"
    const val SESSION_DETAIL = "session_detail/{sessionId}"
    const val ROUTINES = "routines"
    const val ROUTINE_EDITOR = "routine_editor/{routineId}"
    const val WEEKLY_SCHEDULE = "weekly_schedule"
    const val RECOVERY = "recovery"
    const val SWAP = "swap/{mode}/{contextId}/{exerciseId}?routineId={routineId}"
    const val PHOTO_CAPTURE = "photo_capture"
    const val PHOTO_CONFIRMATION = "photo_confirmation/{photoPath}"

    fun exerciseDetail(exerciseId: String) = "exercise_detail/$exerciseId"
    fun exerciseHistory(exerciseId: String) = "exercise_history/$exerciseId"
    fun photoConfirmation(photoPath: String) = "photo_confirmation/${android.net.Uri.encode(photoPath)}"
    fun sessionDetail(sessionId: Long) = "session_detail/$sessionId"
    fun routineEditor(routineId: Long) = "routine_editor/$routineId"
    fun workoutSession(routineId: Long? = null) =
        if (routineId == null) "workout_session" else "workout_session?routineId=$routineId"

    fun swapInSession(sessionId: Long, exerciseId: String, routineId: Long?): String {
        val base = "swap/SESSION/$sessionId/$exerciseId"
        return if (routineId == null) base else "$base?routineId=$routineId"
    }

    fun swapInRoutine(routineId: Long, exerciseId: String) = "swap/ROUTINE/$routineId/$exerciseId"
}

private val TOP_LEVEL_ROUTES = setOf(
    Routes.HOME,
    Routes.WORKOUT_TAB,
    Routes.PROFILE,
)

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val startDestination by rootViewModel.startDestination.collectAsState()
    val resolvedStart = startDestination

    if (resolvedStart == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = backStackEntry?.destination?.route in TOP_LEVEL_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(navController = navController, currentEntry = backStackEntry)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = resolvedStart,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onStartWorkoutClick = { navController.navigateToTopLevel(Routes.WORKOUT_TAB) },
                )
            }
            composable(Routes.WORKOUT_TAB) {
                TodayScreen(
                    onStartRoutine = { routineId ->
                        navController.navigate(Routes.workoutSession(routineId))
                    },
                    onResumeSession = { navController.navigate(Routes.workoutSession()) },
                    onStartEmpty = { navController.navigate(Routes.workoutSession()) },
                    onViewRoutines = { navController.navigate(Routes.ROUTINES) },
                    onViewRecovery = { navController.navigate(Routes.RECOVERY) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onExercisesClick = { navController.navigate(Routes.EXERCISE_BROWSER) },
                    onMeasuresClick = { navController.navigate(Routes.BODY_METRICS) },
                    onCalendarClick = { navController.navigate(Routes.SESSION_HISTORY) },
                    onStatisticsClick = { navController.navigate(Routes.RECOVERY) },
                    onCoachClick = { navController.navigate(Routes.COACH) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.EXERCISE_BROWSER) {
                ExerciseBrowserScreen(
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    onIdentifyEquipmentClick = { navController.navigate(Routes.PHOTO_CAPTURE) },
                )
            }
            composable(Routes.EXERCISE_DETAIL) {
                ExerciseDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onHistoryClick = { id -> navController.navigate(Routes.exerciseHistory(id)) },
                )
            }
            composable(Routes.EXERCISE_HISTORY) {
                ExerciseHistoryScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Routes.PHOTO_CAPTURE) {
                PhotoCaptureScreen(
                    onPhotoProcessed = { photoPath ->
                        navController.navigate(Routes.photoConfirmation(photoPath)) {
                            popUpTo(Routes.PHOTO_CAPTURE) { inclusive = true }
                        }
                    },
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable(Routes.PHOTO_CONFIRMATION) {
                PhotoConfirmationScreen(
                    onSaved = {
                        navController.popBackStack(Routes.EXERCISE_BROWSER, inclusive = false)
                    },
                    onCancel = { navController.popBackStack(Routes.EXERCISE_BROWSER, inclusive = false) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                Routes.WORKOUT_SESSION,
                arguments = listOf(
                    androidx.navigation.navArgument("routineId") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                    }
                ),
            ) { entry ->
                val routineId = entry.arguments?.getString("routineId")?.toLongOrNull()
                val pendingExerciseId = entry.savedStateHandle.get<String>("pickedExerciseId")
                WorkoutSessionScreen(
                    onAddExerciseClick = { navController.navigate(Routes.EXERCISE_PICKER) },
                    onEndSessionClick = { navController.navigateToTopLevel(Routes.SESSION_HISTORY) },
                    onDiscardSession = { navController.navigateToTopLevel(Routes.WORKOUT_TAB) },
                    onSwapClick = { sid, exerciseId, rid ->
                        navController.navigate(Routes.swapInSession(sid, exerciseId, rid))
                    },
                    onExerciseHistoryClick = { id -> navController.navigate(Routes.exerciseHistory(id)) },
                    pendingExerciseId = pendingExerciseId,
                    onPendingExerciseConsumed = {
                        entry.savedStateHandle.remove<String>("pickedExerciseId")
                    },
                    startRoutineId = routineId,
                )
            }
            composable(Routes.EXERCISE_PICKER) {
                ExercisePickerScreen(
                    onExerciseSelected = { id ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("pickedExerciseId", id)
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable(Routes.SESSION_HISTORY) { entry ->
                val justDeleted = entry.savedStateHandle.get<Boolean>("sessionDeleted") == true
                SessionHistoryScreen(
                    onSessionClick = { id -> navController.navigate(Routes.sessionDetail(id)) },
                    onBackClick = { navController.popBackStack() },
                    justDeleted = justDeleted,
                    onDeletedMessageShown = { entry.savedStateHandle.remove<Boolean>("sessionDeleted") },
                )
            }
            composable(Routes.SESSION_DETAIL) {
                SessionDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onDeleted = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("sessionDeleted", true)
                        navController.popBackStack()
                    },
                )
            }
            composable(Routes.BODY_METRICS) {
                BodyMetricsScreen()
            }
            composable(Routes.COACH) {
                CoachChatScreen(
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                )
            }
            composable(Routes.ROUTINES) {
                RoutinesScreen(
                    onRoutineClick = { id -> navController.navigate(Routes.routineEditor(id)) },
                    onScheduleClick = { navController.navigate(Routes.WEEKLY_SCHEDULE) },
                )
            }
            composable(Routes.ROUTINE_EDITOR) { entry ->
                val pendingExerciseId = entry.savedStateHandle.get<String>("pickedExerciseId")
                val routineId = entry.arguments?.getString("routineId")?.toLongOrNull()
                RoutineEditorScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddExerciseClick = { navController.navigate(Routes.EXERCISE_PICKER) },
                    onSwapClick = { exerciseId ->
                        if (routineId != null) {
                            navController.navigate(Routes.swapInRoutine(routineId, exerciseId))
                        }
                    },
                    pendingExerciseId = pendingExerciseId,
                    onPendingExerciseConsumed = {
                        entry.savedStateHandle.remove<String>("pickedExerciseId")
                    },
                )
            }
            composable(Routes.WEEKLY_SCHEDULE) {
                WeeklyScheduleScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Routes.RECOVERY) {
                RecoveryScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                Routes.SWAP,
                arguments = listOf(
                    androidx.navigation.navArgument("routineId") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                    }
                ),
            ) {
                SwapScreen(
                    onDone = { navController.popBackStack() },
                    onBackClick = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController, currentEntry: NavBackStackEntry?) {
    val currentRoute = currentEntry?.destination?.route
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = { navController.navigateToTopLevel(Routes.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.WORKOUT_TAB,
            onClick = { navController.navigateToTopLevel(Routes.WORKOUT_TAB) },
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
            label = { Text("Workout") },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.PROFILE,
            onClick = { navController.navigateToTopLevel(Routes.PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profile") },
        )
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
