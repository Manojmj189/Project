package com.example.androidapp.appActivityWorker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.androidapp.SettingscreenUI.UserPreference
import com.example.androidapp.SettingscreenUI.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

//UI related data is managed by class viewmodel
class  AppSettingViewmodel ( context: Context): ViewModel() {
    private val dataStore = context.dataStore//getting datastore

val activeTime: Flow<Long>//retrieving activetime value from repository
    get()=dataStore.data.map { preferences->preferences[UserPreference.ACTIVETIME]?:0L }

fun setActiveTime(value:Long) {//function to setactivetime value in repository
    viewModelScope.launch {
        dataStore.edit { preferences -> preferences[UserPreference.ACTIVETIME] = value }
    }
}
}

//creating object of appsettingviewmodel by factory class
class AppSettingViewmodelFactory(private val context: Context) : //https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
    ViewModelProvider.Factory {
    override fun <Sup : ViewModel> create(modelClass: Class<Sup>): Sup {
        if (modelClass.isAssignableFrom(AppSettingViewmodel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppSettingViewmodel(context) as Sup
        }
        throw IllegalArgumentException("ViewModel class not recognized")
    }
}
