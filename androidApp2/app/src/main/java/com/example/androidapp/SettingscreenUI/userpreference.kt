package com.example.androidapp.SettingscreenUI

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
//property to enable datastore in context
val Context.dataStore:DataStore<Preferences>by preferencesDataStore(name="Settings")
object UserPreference {//getting user preferences in datastore using object keys
    val MOTIVATION = stringPreferencesKey("motivation")//key to store and retrieval user motivation input
    val ABILITY = stringPreferencesKey("ability")//key to store and retrieval user ability input
    val TRIGGERS_ENAB = booleanPreferencesKey("triggers_enabled")//key to store and retrieval user trigeers enabled input
    val CALORIES_BURNT = stringPreferencesKey("caloriesBurnt")//key to store and retrieval user calorieburnt input
    val MRNGMEALTIME = stringPreferencesKey("mrngMeal_time")//key to store and retrieval user mrngmeal input
    val NOONMEALTIME = stringPreferencesKey("noonMeal_time")//key to store and retrieval user noonmeal input
    val NIGHTMEALTIME = stringPreferencesKey("nightMeal_time")//key to store and retrieval user nightmeal input
    val ACTIVETIME= longPreferencesKey("active_time")//key to store and retrieval user last activity input
}

fun getUserPreferences(context:Context): SettingsViewModel.UserPreferences {
    return runBlocking {//getting first data from store and mapping data class to userpreferences
        val pref = context.dataStore.data.first()
        SettingsViewModel.UserPreferences(
            pref[UserPreference.MOTIVATION] ?: "Low",//default low if motivation and ability not set
            pref[UserPreference.ABILITY] ?: "Low",
             pref[UserPreference.CALORIES_BURNT] ?: "0",
                   pref[UserPreference.MRNGMEALTIME]?:"",//empty default if mealintake not set
                   pref[UserPreference.NOONMEALTIME]?:"",
                   pref[UserPreference.NIGHTMEALTIME]?:"",
                                pref[UserPreference.ACTIVETIME]?:0L)//default 0 if active time is not set
    }
}




