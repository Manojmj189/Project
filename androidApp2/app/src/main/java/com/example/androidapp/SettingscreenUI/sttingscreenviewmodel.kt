package com.example.androidapp.SettingscreenUI

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.androidapp.Notification.MealIntakeWorker
import com.example.androidapp.Notification.NotifiWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

//repository used for storing and retriving user inputs
class SettingsViewModel(private val context: Context):ViewModel() {
    private val dataStore = context.dataStore

    val motivation: Flow<String>//noticing motivation,default low
        get() = dataStore.data.map { preferences -> preferences[UserPreference.MOTIVATION] ?: "Low" }
    val ability: Flow<String>//noticing ability,default low
        get() = dataStore.data.map { preferences -> preferences[UserPreference.ABILITY] ?: "Low" }
    val triggersEnabled: Flow<Boolean>//noticing triggersenabled,default false
        get() = dataStore.data.map { preferences -> preferences[UserPreference.TRIGGERS_ENAB] ?: false }
    val caloriesBurnt: Flow<String>//noticing caloriesBurnt,default 0
        get() = dataStore.data.map { preferences -> preferences[UserPreference.CALORIES_BURNT] ?: "0" }
    val mrngMealTime:Flow<String>//noticing mrngmealtime
        get()=dataStore.data.map{preferences->preferences[UserPreference.MRNGMEALTIME]?:""}
    val noonMealTime:Flow<String>//noticing noonmealtime
        get()=dataStore.data.map { preferences->preferences[UserPreference.NOONMEALTIME]?:"" }
    val nightMealTime:Flow<String>//noticing nightmealtime
        get()= dataStore.data.map{preferences->preferences[UserPreference.NIGHTMEALTIME]?:""}


//data class to grip user prefernces
    data class UserPreferences(
        val motivation: String,
        val ability: String,
        val caloriesBurnt: String,
        val mrngMealTime:String,
        val noonMealTime:String,
        val nightMealTime:String,
        val activeTime:Long)
//setting motivation in repository
    fun setMotivation(value: String) {
        viewModelScope.launch()
        {
            dataStore.edit { preferences -> preferences[UserPreference.MOTIVATION] = value }
            Log.d("SettingsViewModel", "Motivation:$value") } }
    //setting ability in repository
    fun setAbility(value: String) {
        viewModelScope.launch()
        {
            dataStore.edit { preferences -> preferences[UserPreference.ABILITY] = value }
            Log.d("SettingsViewModel", "Ability:$value") } }
    //setting triggersenabled in repository
    fun setTriggersenabled(value: Boolean) {
        viewModelScope.launch()
        {
            dataStore.edit { preferences -> preferences[UserPreference.TRIGGERS_ENAB] = value }
            Log.d("SettingsViewModel", "TriggersEnabled:$value") } }
    //setting caloriesburnt in repository and triggering
    fun setCaloriesBurnt(value: String) {
            viewModelScope.launch() {
                dataStore.edit { preferences -> preferences[UserPreference.CALORIES_BURNT] = value }
                triggerNotification(context)
                Log.d("SettingsViewModel", "caloriesburnt:$value") } }
//setting mrngmealtime in repository
    fun setMrngMealTime(time:String){
        viewModelScope.launch(){
            dataStore.edit{prefernces->prefernces[UserPreference.MRNGMEALTIME]=time}
        Log.d("SettingsViewModel", "mrngmealtime:$time") } }
    //setting noonmealtime in repository
    fun setNoonMealTime(time:String) {
        viewModelScope.launch() {
            dataStore.edit { preferences -> preferences[UserPreference.NOONMEALTIME] = time }
            Log.d("SettingsViewModel", "noonmealtime:$time") }}
    //setting nightmealtime in repository
        fun setNightMealTime(time: String) {
            viewModelScope.launch() {
                dataStore.edit { prefernces -> prefernces[UserPreference.NIGHTMEALTIME] = time }
                Log.d("SettingsViewModel", "nightmealtime:$time") }}


// triggering notification for mealintake using worker
    fun triggerNotification(context: Context) {
                Log.d("SettingsViewModel", "triggernotification")
                val notificatonWorkRequest = OneTimeWorkRequestBuilder<NotifiWorker>().build()
                WorkManager.getInstance(context).enqueue(notificatonWorkRequest)
                val mealworkRequest: WorkRequest = OneTimeWorkRequestBuilder<MealIntakeWorker>().build()
                WorkManager.getInstance(context).enqueue(mealworkRequest)}}

//factory class cfor instances of settinviewmodel
            class SettingViewModelFactory(private val context: Context) : //https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
    ViewModelProvider.Factory {
                override fun <Sup : ViewModel> create(modelClass: Class<Sup>): Sup {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return SettingsViewModel(context) as Sup
                    }
                    throw IllegalArgumentException("ViewModel class not recognized")
                }
            }


