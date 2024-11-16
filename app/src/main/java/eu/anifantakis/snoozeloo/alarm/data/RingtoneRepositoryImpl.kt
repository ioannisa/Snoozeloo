package eu.anifantakis.snoozeloo.alarm.data

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import eu.anifantakis.snoozeloo.alarm.domain.RingtoneRepository
import kotlinx.coroutines.delay
import timber.log.Timber

class RingtoneRepositoryImpl(
    private val context: Context
) : RingtoneRepository {

    private var currentRingtone: Ringtone? = null
    private var currentUri: Uri? = null
    private var isPlaying: Boolean = false

    override fun getDefaultRingtones(): List<Pair<String, Uri>> {
        return try {
            val defaultAlarm = getSystemDefaultAlarmRingtone()
            val ringtoneList = mutableListOf(defaultAlarm) // Add default alarm first

            // Then add other alarm ringtones
            RingtoneManager(context).run {
                setType(RingtoneManager.TYPE_ALARM)
                cursor.use { cursor ->
                    while (cursor.moveToNext()) {
                        try {
                            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                            val uri = getRingtoneUri(cursor.position)
                            // Only add if it's not the default alarm (to avoid duplicates)
                            if (uri != defaultAlarm.second) {
                                ringtoneList.add(title to uri)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to get alarm ringtone at position ${cursor.position}")
                            continue
                        }
                    }
                }
            }

            // Also add general ringtones (as they can be used for alarms too)
            RingtoneManager(context).run {
                setType(RingtoneManager.TYPE_RINGTONE)
                cursor.use { cursor ->
                    while (cursor.moveToNext()) {
                        try {
                            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                            val uri = getRingtoneUri(cursor.position)
                            ringtoneList.add(title to uri)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to get ringtone at position ${cursor.position}")
                            continue
                        }
                    }
                }
            }

            ringtoneList
        } catch (e: Exception) {
            Timber.e(e, "Failed to get default ringtones")
            emptyList()
        }
    }

    override fun getSystemDefaultAlarmRingtone(): Pair<String, Uri> {
        return try {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, defaultUri)
            val title = ringtone?.getTitle(context)?.let { "$it (Default)" } ?: "Default Alarm"
            title to defaultUri
        } catch (e: Exception) {
            Timber.e(e, "Failed to get system default alarm ringtone")
            // Fallback to notification sound if alarm sound isn't available
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val fallbackRingtone = RingtoneManager.getRingtone(context, fallbackUri)
                val title = fallbackRingtone?.getTitle(context)?.let { "$it (Default)" } ?: "Default Notification"
                title to fallbackUri
            } catch (e: Exception) {
                Timber.e(e, "Failed to get fallback notification sound")
                "Default Ringtone" to RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
        }
    }

    override suspend fun play(uri: Uri?) {
        try {
            // If the same ringtone is currently playing, stop it
            if (uri == currentUri && isPlaying) {
                stopPlaying()
                return
            }

            // Stop any previously playing ringtone
            stopPlaying()

            uri?.let {
                val ringtone = RingtoneManager.getRingtone(context, uri)?.also { newRingtone ->
                    currentRingtone = newRingtone
                    currentUri = uri
                    isPlaying = true
                    newRingtone.play()
                } ?: return

                delay(PLAY_DURATION_MS)

                // Stop if still playing after the delay
                if (isPlaying && ringtone.isPlaying) {
                    ringtone.stop()
                    isPlaying = false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to play ringtone")
            stopPlaying()
        }
    }

    override suspend fun stopPlaying() {
        try {
            currentRingtone?.takeIf { it.isPlaying }?.stop()
            currentRingtone = null
            currentUri = null
            isPlaying = false
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop ringtone")
        }
    }

    companion object {
        private const val PLAY_DURATION_MS = 5000L
    }
}