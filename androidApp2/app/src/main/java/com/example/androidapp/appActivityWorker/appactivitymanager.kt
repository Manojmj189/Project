package com.example.androidapp.appActivityWorker

import android.content.Context
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AppActivityMonitor(private val context:Context){//schedulig work is going for appactivitymonitor
    fun appActivityWorkers(){//function to schedule periodicworker
        val appActivityWorkRequest= PeriodicWorkRequestBuilder<AppActivityWorker>(2,TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueue(appActivityWorkRequest)//periodicworker checks every two hours to run appactivityworker
    }
}