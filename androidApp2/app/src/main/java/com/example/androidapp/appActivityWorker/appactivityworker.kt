package com.example.androidapp.appActivityWorker

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
import com.example.androidapp.SettingscreenUI.UserPreference
import com.example.androidapp.SettingscreenUI.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

//appactivity worker executing inactivity time from user
class AppActivityWorker(context: Context, workerParams: WorkerParameters): Worker(context,workerParams) {
    private val applicationContext = context.applicationContext

    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {
        Log.d("AppActivityWorker", "Started")
        return if (isWithinRange() && isUniWiFiConnected() && isBatteryLevelAdequate()) {//checking conditions for triggering appactivity worker
            Log.d("AppActivityWorker ", "Conditions met")
            displayAppActivityNotification()
            Result.success()
        } else {
            Log.d("AppActivityWorker", "not met.Details")
            Log.d("AppActivityWorker", "Range:isWithinRange")
            Log.d("AppActivityWorker", "Battery:BatteryLevelAdequate")
            Result.failure()
        }
    }


    private fun isWithinRange(): Boolean {//checking the time range between 9am to 9 pm
        val calender = Calendar.getInstance()
        val currenthour = calender.get(Calendar.HOUR_OF_DAY)
        val withinRange = currenthour in 9..21
        Log.d("appactivityworker", "isWithinRange:$withinRange(Current hour:currentHour)")
        return withinRange

    }

    private fun isUniWiFiConnected(): Boolean {//checking weather connected to uniwifi eduroam
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return if (networkCapabilities != null) {
            Log.d("AppActivityWorker", "Network capabilities not null")
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifi =
                    applicationContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val information = wifi.connectionInfo
                val wifiname = information.ssid
                Log.d("AppActivityWorker", "WiFi ssid:$wifiname")
                if (wifiname == "\"eduroam\"") {
                    Log.d("AppActivityWorker", "Connected to Univ Wifi")
                    true
                } else {
                    Log.d("AppActivityWorker", "not connected")
                    false
                }
            } else {
                Log.d("AppActivityWorker", "not having wifi transport")
                false
            }
        } else {
            Log.d("AppActivityWorker", "net capabilities are nul")
            false
        }
        return false
    }

    private fun isBatteryLevelAdequate(): Boolean {//checking battery level which is above 20
        val batteryManager =
            applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val Adequate = batteryLevel > 20
        Log.d("AppActivityWorker", "BatteryLevelAdequate:Adequate(Current level:batteryLevel)")
        return Adequate
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayAppActivityNotification() {
        val dataStore = applicationContext.dataStore
        val activeTime = runBlocking { dataStore.data.first()[UserPreference.ACTIVETIME] ?: 0 }
        val presentTime = System.currentTimeMillis()
        val Time = TimeUnit.DAYS.toMillis(2)//setting limit for 2 days

        if (presentTime - activeTime > Time) {//this function checks the user weather he inactive for more thsn 2 days
            setupNotificationChannel()////setting up notification channel for the app activity worker
            val notificationId = 4
            val notificationBuilder =
                NotificationCompat.Builder(applicationContext, "appactivity_channel")
                    .setContentTitle("appinactivity")//
                    .setContentText("You have been inacative for 2 days,work on with the app to stay fit!")
                    .setSmallIcon(R.drawable.logo)//fixing icon for app
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .addAction(
                        R.drawable.logo, "Adjust Settings", PendingIntent.getActivity(
                            applicationContext,//allows user to adjust triggers
                            0,
                            Intent(applicationContext, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .addAction(//allows user to stop triggers
                        R.drawable.logo, "Stop Triggers", PendingIntent.getBroadcast(
                            applicationContext,
                            0,
                            Intent(applicationContext, CancelTriggerReceiver::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
            with(NotificationManagerCompat.from(applicationContext)) {
                if (ActivityCompat.checkSelfPermission(
                        applicationContext, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                notify(notificationId, notificationBuilder.build())
            }
        }
    }
//https://developer.android.com/develop/ui/views/notifications/channels
    @RequiresApi(Build.VERSION_CODES.O)//function to set up notificationn channel
    fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_0_1) {
            val ChannelName = "appactivity Channel"
            val ChannelDescription = "Channel for remainders"
            val channelImportance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel =
                NotificationChannel("appactivity_channel", ChannelName, channelImportance).apply {
                    description = ChannelDescription
                }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

    }


    class CancelTriggerReceiver : BroadcastReceiver() {//broadcast receiver to handle stop triggers
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
