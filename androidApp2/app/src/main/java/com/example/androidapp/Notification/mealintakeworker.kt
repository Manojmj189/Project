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
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.androidapp.MainActivity
import com.example.androidapp.R
import com.example.androidapp.SettingscreenUI.getUserPreferences
import java.util.Calendar

//worker executing mainmethod
class MealIntakeWorker(context: Context, workerParams: WorkerParameters): Worker(context,workerParams) {


    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {
        Log.d("MealIntakeWorker", "Started")
        return if (WithinRange() && UniWiFiConnected() && BatteryLevelAdequate()) {//condtions to trigger mealintake
            Log.d("MealIntakeWorker", "Conditions met")
            displayMealIntakeNotification()
            Result.success()
        } else {
            Log.d("MealIntakeWorker", "not met.Details")
            Log.d("MealIntakeWorker", "Range:isWithinRange")
            Log.d("MealIntakeWorker", "Battery:BatteryLevelAdequate")
            Result.failure()
        }
    }


    private fun WithinRange(): Boolean {//checking  the time if its 9am to 9pm
        val calender = Calendar.getInstance()
        val currenthour = calender.get(Calendar.HOUR_OF_DAY)
        val withinRange = currenthour in 9..21
        Log.d("MealIntakeWorker", "WithinRange:$withinRange(Current hour:currentHour)")
        return withinRange

    }

    private fun UniWiFiConnected(): Boolean {//checking weather its a uni wifi eduroam and connecting
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return if (networkCapabilities != null) {
            Log.d("MealIntakeWorker", "Network capabilities not null")
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifi =
                    applicationContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val information = wifi.connectionInfo
                val wifiname = information.ssid
                Log.d("NotifiWorker", "WiFi ssid:$wifiname")
                if (wifiname == "\"eduroam\"") {
                    Log.d("MealIntakeWorker", "Connected to Univ Wifi")
                    true
                } else {
                    Log.d("MealIntakeWorker", "not connected")
                    false
                }
            } else {
                Log.d("MealIntakeWorker", "not having wifi transport")
                false
            }
        } else {
            Log.d("MealIntakeWorker", "network capabilities are nul")
            false
        }
        return false
    }

    private fun BatteryLevelAdequate(): Boolean {//checking batter level is above 20
        val batteryManager =
            applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val Adequate = batteryLevel > 20
        Log.d("MealIntakeWorker", "BatteryLevelAdequate:Adequate(Current level:batteryLevel)")
        return Adequate
    }

    @RequiresApi(Build.VERSION_CODES.O)//conditons for user based on this user will get notification for meal time
    private fun displayMealIntakeNotification() {
        val userPreferences = getUserPreferences(applicationContext)
        val motivation = userPreferences.motivation
        val ability=userPreferences.ability
        val presentTime=Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val mealtimeMessage=when {
            motivation=="High"&&ability=="High"->{
                getMealtimeMessage(presentTime,9,12,19,"Stay consistent with your meal","Good Job!.You have completed your breakfast","Good Job!.You have completed your Lunch","Good Job!.You have completed your Dinner")
            }
            motivation=="Low"&&ability=="Low"->{
                getMealtimeMessage(presentTime,12,15,21,"","Late for breakfast,try to be quick","Late for Lunch,try to be quick","Late for Dinner,try to be quick")
            }
            (motivation=="High"&&ability=="Low")||(motivation=="Low"&&ability=="High")->{
                getMealtimeMessage(presentTime,11,14,20,"","Remember to eat breakfast,even if your busy","Remember to eat dinner,even if your busy","Remember to eat dinner,even if your busy")
            }
            else-> "no meal schedule"
        }

        mealtimeMessage.let {//
            setupNotificationChannel()
            val notificationId = 3
            val notificationBuilder =
                NotificationCompat.Builder(applicationContext, "meal_intake_channel")
                    .setContentTitle("Meal Intake")
                    .setContentText(it)
                    .setSmallIcon(R.drawable.logo)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .addAction(
                        R.drawable.logo, "Adjust Settings", PendingIntent.getActivity(
                            applicationContext,
                            0,
                            Intent(applicationContext, MainActivity::class.java),
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
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                {
                    return
                }
                notify(notificationId, notificationBuilder.build())
            }
        }
    }
    private fun getMealtimeMessage(//defining present time and prefered threshold
        presentTime:Int,breakfastThreshold:Int,lunchThreshold:Int,dinnerthreshold:Int,
        delaymealMessage: String,earlymrngmealMessage:String,earlynoonMessage:String,earlynightMessage:String):String{
        return when{
            presentTime<=breakfastThreshold->earlymrngmealMessage
            presentTime<=lunchThreshold->earlynoonMessage
            presentTime<=dinnerthreshold->earlynightMessage
            else->delaymealMessage
        }
    }

//https://developer.android.com/develop/ui/views/notifications/channels
    @RequiresApi(Build.VERSION_CODES.O)//setting up notfication channel for mealintake
    fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_0_1) {
            val ChannelName = "Meal Intake Notification Channel"
            val ChannelDescription = "Channel for Meal Intake"
            val channelImportance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel =
                NotificationChannel("meal_intake_channel", ChannelName, channelImportance).apply {
                    description = ChannelDescription
                }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

    }


    class CancelTriggerReceiver : BroadcastReceiver() {//condition and function for user to stop trigger
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