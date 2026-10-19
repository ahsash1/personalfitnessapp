package com.example.personalfitnessapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.personalfitnessapp.ui.stats.CalendarScreen
import com.example.personalfitnessapp.ui.stats.StatisticsScreen
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
import com.example.personalfitnessapp.ui.settings.AiConnectionScreen
import com.example.personalfitnessapp.ui.settings.SettingsScreen
import com.example.personalfitnessapp.ui.swap.SwapScreen
import com.example.personalfitnessapp.ui.workout.ActiveWorkoutScreen
import com.example.personalfitnessapp.ui.workout.AddExerciseScreen
import com.example.personalfitnessapp.ui.workout.CelebrationScreen
import com.example.personalfitnessapp.ui.workout.EditWorkoutScreen
import com.example.personalfitnessapp.ui.workout.HomeFeedScreen
import com.example.personalfitnessapp.ui.workout.MenuNav
import com.example.personalfitnessapp.ui.workout.PickerResult
import com.example.personalfitnessapp.ui.workout.SaveWorkoutScreen
import com.example.personalfitnessapp.ui.workout.WorkoutDetailScreen
import com.example.personalfitnessapp.ui.workout.WorkoutPill
import com.example.personalfitnessapp.ui.workout.WorkoutPillViewModel
import com.example.personalfitnessapp.ui.workout.WorkoutSettingsScreen
import com.example.personalfitnessapp.ui.workout.WorkoutTabScreen

/**
 * Bottom nav is 3 tabs (Home / Workout / Profile) per docs/00_overview.md. The live workout
 * is a full-screen route; minimizing it pops back to the tabs, where [WorkoutPill] floats
 * above the bottom nav (docs/screens/active_workout.md §16).
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val WORKOUT_TAB = "workout_tab"
    const val PROFILE = "profile"
    const val EXERCISE_BROWSER = "exercise_browser"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}?add={add}&tab={tab}"
    const val SETTINGS = "settings"
    const val ACTIVE_WORKOUT = "active_workout"
    const val EDIT_WORKOUT = "edit_workout/{sessionId}"
    const val ADD_EXERCISE = "add_exercise?single={single}"
    const val SAVE_WORKOUT = "save_workout"
    const val CELEBRATION = "celebration/{sessionId}"
    const val EXERCISE_PICKER = "exercise_picker"
    const val CALENDAR = "calendar"
    const val WORKOUT_SETTINGS = "workout_settings"
    const val STATISTICS = "statistics"
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
    const val AI_CONNECTION = "ai_connection"

    fun exerciseDetail(exerciseId: String, add: Boolean = false) = "exercise_detail/$exerciseId?add=$add&tab=summary"
    fun photoConfirmation(photoPath: String) = "photo_confirmation/${android.net.Uri.encode(photoPath)}"
    fun sessionDetail(sessionId: Long) = "session_detail/$sessionId"
    fun routineEditor(routineId: Long) = "routine_editor/$routineId"
    fun editWorkout(sessionId: Long) = "edit_workout/$sessionId"
    fun addExercise(single: Boolean) = "add_exercise?single=$single"
    fun celebration(sessionId: Long) = "celebration/$sessionId"

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
    val route = backStackEntry?.destination?.route
    val showBottomBar = route in TOP_LEVEL_ROUTES || route in setOf(Routes.SESSION_DETAIL, Routes.STATISTICS, Routes.CALENDAR)
    val pillViewModel: WorkoutPillViewModel = hiltViewModel()
    val activeWorkout by pillViewModel.active.collectAsState()
    val menuNav = MenuNav(
        onCopied = { navController.navigate(Routes.ACTIVE_WORKOUT) },
        onEdit = { id -> navController.navigate(Routes.editWorkout(id)) },
        onRoutineSaved = { id -> navController.navigate(Routes.routineEditor(id)) },
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    if (activeWorkout != null) WorkoutPill(onOpen = { navController.navigate(Routes.ACTIVE_WORKOUT) }, viewModel = pillViewModel)
                    AppBottomBar(navController = navController, currentEntry = backStackEntry)
                }
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
                HomeFeedScreen(
                    onWorkoutClick = { id -> navController.navigate(Routes.sessionDetail(id)) },
                    onStartWorkout = { navController.navigateToTopLevel(Routes.WORKOUT_TAB) },
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                    onCoach = { navController.navigate(Routes.COACH) },
                    nav = menuNav,
                )
            }
            composable(Routes.WORKOUT_TAB) {
                WorkoutTabScreen(
                    onOpenWorkout = { navController.navigate(Routes.ACTIVE_WORKOUT) },
                    onRoutines = { navController.navigate(Routes.ROUTINES) },
                    onCoach = { navController.navigate(Routes.COACH) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onExercisesClick = { navController.navigate(Routes.EXERCISE_BROWSER) },
                    onMeasuresClick = { navController.navigate(Routes.BODY_METRICS) },
                    onCalendarClick = { navController.navigate(Routes.CALENDAR) },
                    onStatisticsClick = { navController.navigate(Routes.STATISTICS) },
                    onRecoveryClick = { navController.navigate(Routes.RECOVERY) },
                    onCoachClick = { navController.navigate(Routes.COACH) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    onWorkoutClick = { id -> navController.navigate(Routes.sessionDetail(id)) },
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                    nav = menuNav,
                )
            }
            composable(Routes.STATISTICS) {
                StatisticsScreen(
                    onBack = { navController.popBackStack() },
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    onBack = { navController.popBackStack() },
                    onWorkoutClick = { id -> navController.navigate(Routes.sessionDetail(id)) },
                )
            }
            composable(Routes.EXERCISE_BROWSER) {
                ExerciseBrowserScreen(
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    onIdentifyEquipmentClick = { navController.navigate(Routes.PHOTO_CAPTURE) },
                )
            }
            composable(
                Routes.EXERCISE_DETAIL,
                arguments = listOf(
                    androidx.navigation.navArgument("add") { type = androidx.navigation.NavType.StringType; defaultValue = "false" },
                    androidx.navigation.navArgument("tab") { type = androidx.navigation.NavType.StringType; defaultValue = "summary" },
                ),
            ) {
                ExerciseDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onWorkoutClick = { id -> navController.navigate(Routes.sessionDetail(id)) },
                    onAdd = { id ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(SELECT_KEY, id)
                        navController.popBackStack()
                    },
                )
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
                SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onAiConnectionClick = { navController.navigate(Routes.AI_CONNECTION) },
                    onWorkoutSettingsClick = { navController.navigate(Routes.WORKOUT_SETTINGS) },
                )
            }
            composable(Routes.WORKOUT_SETTINGS) {
                WorkoutSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.AI_CONNECTION) {
                AiConnectionScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Routes.ACTIVE_WORKOUT) { entry ->
                ActiveWorkoutScreen(
                    picker = entry.pickerResult(),
                    onPickerConsumed = { entry.clearPickerResult() },
                    onMinimize = { navController.popBackStack() },
                    onFinish = { navController.navigate(Routes.SAVE_WORKOUT) },
                    onAddExercise = { navController.navigate(Routes.addExercise(single = false)) },
                    onReplaceExercise = { navController.navigate(Routes.addExercise(single = true)) },
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                    onSettings = { navController.navigate(Routes.WORKOUT_SETTINGS) },
                    onCoach = { navController.navigate(Routes.COACH) },
                    onDiscarded = { navController.popBackStack() },
                )
            }
            composable(Routes.EDIT_WORKOUT) { entry ->
                EditWorkoutScreen(
                    picker = entry.pickerResult(),
                    onPickerConsumed = { entry.clearPickerResult() },
                    onClose = { navController.popBackStack() },
                    onAddExercise = { navController.navigate(Routes.addExercise(single = false)) },
                    onReplaceExercise = { navController.navigate(Routes.addExercise(single = true)) },
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                )
            }
            composable(
                Routes.ADD_EXERCISE,
                arguments = listOf(
                    androidx.navigation.navArgument("single") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "false"
                    }
                ),
            ) { entry ->
                val single = entry.arguments?.getString("single") == "true"
                val selectRequest by entry.savedStateHandle.getStateFlow<String?>(SELECT_KEY, null).collectAsState()
                AddExerciseScreen(
                    selectRequest = selectRequest,
                    onSelectConsumed = { entry.savedStateHandle[SELECT_KEY] = null },
                    onCancel = { navController.popBackStack() },
                    onDone = { ids ->
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            if (single) handle[REPLACEMENT_KEY] = ids.first() else handle[PICKED_KEY] = ArrayList(ids)
                        }
                        navController.popBackStack()
                    },
                    onInfo = { id -> navController.navigate(Routes.exerciseDetail(id, add = true)) },
                )
            }
            composable(Routes.SAVE_WORKOUT) {
                SaveWorkoutScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.navigate(Routes.celebration(id)) {
                            popUpTo(Routes.ACTIVE_WORKOUT) { inclusive = true }
                        }
                    },
                    onDiscarded = { navController.popBackStack(Routes.ACTIVE_WORKOUT, inclusive = true) },
                )
            }
            composable(Routes.CELEBRATION) {
                CelebrationScreen(onDone = { navController.navigateToTopLevel(Routes.HOME) })
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
            composable(Routes.SESSION_DETAIL) {
                WorkoutDetailScreen(
                    onBack = { navController.popBackStack() },
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                    nav = MenuNav(
                        onCopied = menuNav.onCopied,
                        onEdit = menuNav.onEdit,
                        onRoutineSaved = menuNav.onRoutineSaved,
                        onDeleted = { navController.popBackStack() },
                    ),
                )
            }
            composable(Routes.BODY_METRICS) {
                BodyMetricsScreen()
            }
            composable(Routes.COACH) {
                CoachChatScreen(
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                    onAiConnectionClick = { navController.navigate(Routes.AI_CONNECTION) },
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
                RecoveryScreen(
                    onBackClick = { navController.popBackStack() },
                    onAskCoach = { navController.navigate(Routes.COACH) },
                )
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

private const val PICKED_KEY = "pickedExerciseIds"
private const val SELECT_KEY = "selectExercise"
private const val REPLACEMENT_KEY = "replacementExerciseId"

@Composable
private fun NavBackStackEntry.pickerResult(): PickerResult {
    val added by savedStateHandle.getStateFlow<ArrayList<String>?>(PICKED_KEY, null).collectAsState()
    val replacement by savedStateHandle.getStateFlow<String?>(REPLACEMENT_KEY, null).collectAsState()
    return PickerResult(added, replacement)
}

private fun NavBackStackEntry.clearPickerResult() {
    savedStateHandle[PICKED_KEY] = null
    savedStateHandle[REPLACEMENT_KEY] = null
}
