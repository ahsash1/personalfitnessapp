package com.example.personalfitnessapp

import android.app.Application
import com.example.personalfitnessapp.data.importer.BundledWorkoutImport
import com.example.personalfitnessapp.data.seed.ExerciseSeeder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class FitnessApp : Application() {
    @Inject lateinit var exerciseSeeder: ExerciseSeeder
    @Inject lateinit var bundledImport: BundledWorkoutImport

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            exerciseSeeder.seedIfNeeded()
            runCatching { bundledImport.runOnce() }
            runCatching { bundledImport.refreshRecordsOnce() }
            runCatching { bundledImport.circuitsOnce() }
        }
    }
}
