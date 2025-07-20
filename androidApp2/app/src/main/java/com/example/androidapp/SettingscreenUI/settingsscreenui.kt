package com.example.androidapp.SettingscreenUI

import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.appActivityWorker.AppSettingViewmodel
import com.example.androidapp.appActivityWorker.AppSettingViewmodelFactory
import java.util.Calendar


@Preview
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel=viewModel(factory=SettingViewModelFactory(LocalContext.current)),
                   appSettingViewModel: AppSettingViewmodel =viewModel(factory= AppSettingViewmodelFactory(LocalContext.current))){
    //getting the currenet context
    val context = LocalContext.current
    val motivation by settingsViewModel.motivation.collectAsState(initial = "Low")//noticing motivation state
    val ability by settingsViewModel.ability.collectAsState(initial = "Low")//noticing ability state
    val triggersEnabled by settingsViewModel.triggersEnabled.collectAsState(initial = false)//noticing triggers enabled
    val caloriesBurnt by settingsViewModel.caloriesBurnt.collectAsState(initial = "0")//noticing caloriesburnt from viewmodel state
    val showAdditionalFields = remember { mutableStateOf(false) }//holds data of calories burnt
    val CaloriesBurntintake = remember { mutableStateOf("") }//storing mealintake input
    val preferedTime= remember { mutableStateOf("") }//storing specified meal time
    val preferedMealTime= remember { mutableStateOf("") }//storing morningmeal time
    val mrngMealTime by settingsViewModel.mrngMealTime.collectAsState(initial = "")//noticing mrngmealtime from viewmodel
    val noonMealTime by settingsViewModel.noonMealTime.collectAsState(initial = "")//noticing noonmealtime from viewmodel
    val nightMealTime by settingsViewModel.nightMealTime.collectAsState(initial = "")//noticing nightmealtime from viewmodel


    AppActivityRecondition(appSettingViewModel)//updating activetime
    Column(//column layout for settingpage
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)
    )
    {
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)

        Text(text = "Motivation")//motivation settings for settingscreen using radio button
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = motivation == "Low",
                onClick = { settingsViewModel.setMotivation("Low")
                    })
            Text(text = "Low")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = motivation == "High",
                onClick = { settingsViewModel.setMotivation("High")
                    })
            Text(text = "High")
        }

        Text(text = "Ability")//ability settings for settingscreen using radio button
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = ability == "Low", onClick = { settingsViewModel.setAbility("Low")
                })
            Text(text = "Low")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = ability == "High", onClick = { settingsViewModel.setAbility("High")
                })
            Text(text = "High")
        }

        Text(text = "TriggersEnabled")//enabling/disabling triggers using checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = triggersEnabled,
                onCheckedChange = {
                    settingsViewModel.setTriggersenabled(it)
                    showAdditionalFields.value=it
                    Log.d("SettingScreen", "TriggersEnabled:$it")
                })
        }

        if (showAdditionalFields.value) {//showing additional triggers when triggerenabled checkbox is selected
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "CaloriesBurnt", style = MaterialTheme.typography.bodyLarge)
            TextField(
                value = CaloriesBurntintake.value,
                onValueChange = { CaloriesBurntintake.value = it},
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(11.dp))
            Text(text = "Meal Time", style = MaterialTheme.typography.bodyLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            )
            {
                RadioButton(selected = preferedMealTime.value == "Breakfast", onClick = {//mealtime category selectiion using radio buttons
                    preferedMealTime.value = "Breakfast"
                    displayTimePickerDialog(context, preferedTime,settingsViewModel,"Breakfast")
                })
                Text(text = "Breakfast:$mrngMealTime")
                RadioButton(selected = preferedMealTime.value == "Lunch", onClick = {
                    preferedMealTime.value = "Lunch"
                    displayTimePickerDialog(context, preferedTime,settingsViewModel,"Lunch")
                })
                Text(text = "Lunch:$noonMealTime")
                RadioButton(selected = preferedMealTime.value == "Dinner", onClick = {
                    preferedMealTime.value = "Dinner"
                    displayTimePickerDialog(context, preferedTime,settingsViewModel,"Dinner")
                })
                Text(text = "Dinner:$nightMealTime")
            }
                if (preferedTime.value.isNotEmpty()) {//showing prefered time
                    Text(text = "preferedTime:${preferedTime.value}")
                }
                Button(
                    onClick = {//submit button to update the triggers
                        if (preferedMealTime.value.isNotEmpty()){
                        settingsViewModel.setCaloriesBurnt(CaloriesBurntintake.value)
                        settingsViewModel.triggerNotification(context)
                        Log.d("SettingScreen", "calories input") } },
                    modifier = Modifier.align(Alignment.CenterHorizontally))
                {
                    Text(text = "Submit")
                }
            }
        }
    }
     @Composable//fun to track inactivity of user
     fun AppActivityRecondition(appSettingViewModel: AppSettingViewmodel){
         LaunchedEffect(Unit) {
             appSettingViewModel.setActiveTime(System.currentTimeMillis())
         }
     }
//displaying clock using timepickerfunction fun to selecct time for meal notification
fun displayTimePickerDialog(context: Context,preferedTime:MutableState<String>,viewModel: SettingsViewModel,differentMeal:String){//https://developer.android.com/reference/android/app/TimePickerDialog
    val cal=Calendar.getInstance()
    val hour=cal.get(Calendar.HOUR_OF_DAY)
    val timing=cal.get(Calendar.MINUTE)
    TimePickerDialog(context,{_,preferedHour,preferedTiming->val time=String.format("%02d:%02d",preferedHour,preferedTiming)
        preferedTime.value=time
    when (differentMeal){
        "Breakfast"-> viewModel.setMrngMealTime(time)
        "Lunch"->viewModel.setNoonMealTime(time)
        "Dinner"->viewModel.setNightMealTime(time)
    }
    },hour,timing,true).show()
     }






