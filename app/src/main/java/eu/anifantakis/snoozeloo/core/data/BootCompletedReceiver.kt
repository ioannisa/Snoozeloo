package eu.anifantakis.snoozeloo.core.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class BootCompletedReceiver : BroadcastReceiver(), KoinComponent {
    private val alarmsRepository: AlarmsRepository by inject()
    private val alarmScheduler: AlarmScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        goAsync().apply {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    Timber.d("Rescheduling alarms after boot...")

                    val alarms = alarmsRepository.getAlarms()
                        .first()
                        .filter { it.isEnabled }

                    alarms.forEach { alarm ->
                        alarmScheduler.schedule(alarm)
                        Timber.d("Rescheduled alarm ${alarm.id} for ${alarm.hour}:${alarm.minute}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to reschedule alarms after boot")
                } finally {
                    finish()
                    cancel()
                }
            }
        }
    }
}
