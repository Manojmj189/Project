package com.example.androidapp


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.androidapp.appActivityWorker.AppActivityMonitor
import com.example.androidapp.Notification.MealIntakeWorker
import com.example.androidapp.Notification.NotifiWorker
import com.example.androidapp.SettingscreenUI.SettingsScreen
import com.example.androidapp.ui.theme.AndroidAppTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidAppTheme {
                SettingsScreen()
            }
        }
        caloriesAndMealWorkers()
    }

    private fun caloriesAndMealWorkers() {
        val calorieworkRequest: WorkRequest =
            PeriodicWorkRequestBuilder<NotifiWorker>(2, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueue(calorieworkRequest)
        val mealworkRequest: WorkRequest =
            PeriodicWorkRequestBuilder<MealIntakeWorker>(2, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueue(mealworkRequest)
        val appActivityMonitor = AppActivityMonitor(applicationContext)
        appActivityMonitor.appActivityWorkers()
    }
}
