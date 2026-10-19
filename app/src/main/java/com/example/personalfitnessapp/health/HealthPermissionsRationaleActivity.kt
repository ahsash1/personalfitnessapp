package com.example.personalfitnessapp.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary

/** What Health Connect shows when the user taps this app's privacy policy link. */
class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(Modifier.fillMaxSize().background(AppBackground).statusBarsPadding().padding(24.dp)) {
                Text("How this app uses your health data", color = AppTextPrimary, fontSize = 22.sp)
                Text(
                    "It reads your steps and active calories (for example from Samsung Health) only to show " +
                        "how many calories you burned each day next to the food you logged. Nothing is written " +
                        "back, nothing leaves your phone, and there is no account or cloud sync.",
                    Modifier.padding(top = 12.dp), color = AppTextSecondary, fontSize = 16.sp,
                )
            }
        }
    }
}
