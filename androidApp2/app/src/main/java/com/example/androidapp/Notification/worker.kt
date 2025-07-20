package com.example.androidapp.Notification


import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.androidapp.MainActivity
import com.example.androidapp.R
import java.util.Calendar
import com.example.androidapp.SettingscreenUI.getUserPreferences

//worker executing mainmethod for calorieburnt

class NotifiWorker(context:Context,workerParams:WorkerParameters):Worker(context,workerParams) {


    @RequiresApi(VERSION_CODES.O)
    override fun doWork(): Result {
        Log.d("NotifiWorker", "Started")
        return if (WithinRange() && UniWiFiConnected() && BatteryLevelAdequate()) {//condtions to trigger mealintake
            Log.d("NotifiWorker", "Conditions met")
            displayNotification()
            Result.success()
        } else {
            Log.d("NotifiWorker", "not met.Details")
            Log.d("NotifiWorker", "Range:isWithinRange")
            Log.d("NotifiWorker", "Battery:BatteryLevelAdequate")
            Result.failure()
        }
    }


    private fun WithinRange(): Boolean {//checking time range betweeen 9am to 9pm to connect
        val calender = Calendar.getInstance()
        val currenthour = calender.get(Calendar.HOUR_OF_DAY)
        val withinRange = currenthour in 9..21
        Log.d("NotifiWorker", "WithinRange:$withinRange(Current hour:currentHour)")
        return withinRange

    }

    private fun UniWiFiConnected(): Boolean {//checking weather uni wifi is connected in the name of eduroam
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return if (networkCapabilities != null) {
            Log.d("NotifiWorker", "Network capabilities not null")
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifi =
                    applicationContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val information = wifi.connectionInfo
                val wifiname = information.ssid
                Log.d("NotifiWorker", "WiFi ssid:$wifiname")
                if (wifiname == "\"eduroam\"") {
                    Log.d("NotifiWorker", "Connected to Univ Wifi")
                    true
                } else {
                    Log.d("NotifiWorker", "not connected")
                    false
                }
            } else {
                Log.d("NotifiWorker", "not having wifi transport")
                false
            }
        } else {
            Log.d("NotifiWorker", "network capabilities are nul")
            false
        }
        return false
    }

    private fun BatteryLevelAdequate(): Boolean {//checking battery level is above 20
        val batteryManager =
            applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val Adequate = batteryLevel > 20
        Log.d("NotifiWorker", "BatteryLevelAdequate:Adequate(Current level:batteryLevel)")
        return Adequate
    }

    @RequiresApi(VERSION_CODES.O)//its a main conditions for the user based on this user will get notification
    private fun displayNotification() {
        val userPreferences = getUserPreferences(applicationContext)
        val burntCalories = userPreferences.caloriesBurnt.toIntOrNull() ?: 0

        val desiredCalories = when {
            userPreferences.ability == "High" && userPreferences.motivation == "High" -> 500
            userPreferences.ability == "Low" && userPreferences.motivation == "Low" -> 200
            userPreferences.ability == "High" && userPreferences.motivation == "Low" -> 300
            userPreferences.ability == "Low" && userPreferences.motivation == "High" -> 300
            else -> 0
        }
        val remainingCalories = desiredCalories - burntCalories
        Log.d(
            "NotifiWorker",
            "userMotivation:$userPreferences.motivation,Ability:$userPreferences.ability"
        )
        Log.d(
            "NotifiWorker",
            "burntCalories:$burntCalories,desiredCalories:$desiredCalories,remainingCalories:$remainingCalories"
        )

        val displayNotification = if (burntCalories >= desiredCalories) {
            "Congralutaions! Calories burnt goal accomplished"
        } else {
            "Still $remainingCalories Calories needed to be burnt"
        }


        setupNotificationChannel()//settingup notification channel for calories burnt
        val notificationId = 2
        val notificationBuilder = NotificationCompat.Builder(applicationContext, "default_channel")
            .setContentTitle("Calories Burnt")
            .setContentText("$displayNotification")
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.logo, "Adjust Settings", PendingIntent.getActivity(
                    applicationContext, 0, Intent(applicationContext, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                R.drawable.logo, "Stop Triggers", PendingIntent.getBroadcast(
                    applicationContext,
                    0,
                    Intent(applicationContext, CancelTriggerReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
            }
            notify(notificationId, notificationBuilder.build())
        }
    }
//https://developer.android.com/develop/ui/views/notifications/channels
    @RequiresApi(VERSION_CODES.O)
    fun setupNotificationChannel() {//setting up channel name and other things
        if (Build.VERSION.SDK_INT >= VERSION_CODES.ECLAIR_0_1) {
            val ChannelName = "Default Notification Channel"
            val ChannelDescription = "Channel for behavior change reminders"
            val channelImportance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel =
                NotificationChannel("default_channel", ChannelName, channelImportance).apply {
                    description = ChannelDescription
                }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

    }


    class CancelTriggerReceiver : BroadcastReceiver() {//canceling triggers based on users wish by clicking stop triggers
        override fun onReceive(cont: Context, int: Intent) {
            WorkManager.getInstance(cont).cancelAllWork()
            val sharedPref = cont.getSharedPreferences("settings", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("triggers_enabled", false)
                apply()
            }
        }
    }
}
