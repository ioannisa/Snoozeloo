package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.datasource.AlarmId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmEditViewModel(
    private val repository: AlarmsRepository
): ViewModel() {

    private val _alarm = MutableStateFlow<Alarm?>(null)
    val alarm = _alarm.asStateFlow()

    fun loadAlarm(id: AlarmId) {
        viewModelScope.launch {
            try {
                val loadedAlarm = repository.getAlarm(id)
                _alarm.value = loadedAlarm
            } catch (e: Exception) {
                // Handle error case
            }

            delay(1500L)
            updateAlarmTime(2,3)
        }
    }

    fun updateAlarmTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            _alarm.value?.let { currentAlarm ->
                val updatedAlarm = currentAlarm.copy(
                    hour = hour,
                    minute = minute
                )
                repository.upsertAlarm(updatedAlarm)
            }
        }
    }

}