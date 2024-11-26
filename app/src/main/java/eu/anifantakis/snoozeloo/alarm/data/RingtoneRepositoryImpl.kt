package eu.anifantakis.snoozeloo.alarm.data

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import eu.anifantakis.snoozeloo.alarm.domain.RingtoneRepository
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Implementation of [RingtoneRepository] that manages system ringtones.
 * Handles ringtone discovery, playback, and system audio integration.
 *
 * Key features:
 * - Lists available system ringtones and alarms
 * - Manages ringtone playback with preview duration
 * - Handles fallback paths for unavailable sounds
 * - Maintains playback state
 *
 * @property context Application context for accessing system services
 */
class RingtoneRepositoryImpl(
    private val context: Context
) : RingtoneRepository {

    // Playback state management
    private var currentRingtone: Ringtone? = null
    private var currentUri: Uri? = null
    private var isPlaying: Boolean = false

    /**
     * Retrieves all available ringtones from the system.
     * Combines alarm sounds and general ringtones into a single list.
     *
     * Process:
     * 1. Gets system default alarm
     * 2. Adds all alarm-specific sounds
     * 3. Adds general ringtones as additional options
     *
     * @return List of ringtone title and URI pairs, empty if access fails
     */
    override fun getAllRingtones(): List<Pair<String, Uri>> {
        return try {
            val defaultAlarm = getSystemDefaultAlarmRingtone()
            val ringtoneList = mutableListOf(defaultAlarm) // Start with default alarm

            // Add alarm-specific ringtones
            RingtoneManager(context).run {
                setType(RingtoneManager.TYPE_ALARM)
                cursor.use { cursor ->
                    while (cursor.moveToNext()) {
                        try {
                            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                            val uri = getRingtoneUri(cursor.position)
                            // Avoid duplicate of default alarm
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

            // Add general ringtones as additional options
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

    /**
     * Gets the system's default alarm ringtone with fallback options.
     * Falls back to notification sound, then general ringtone if alarm sound unavailable.
     *
     * @return Pair of ringtone title and URI
     */
    override fun getSystemDefaultAlarmRingtone(): Pair<String, Uri> {
        return try {
            // Try alarm sound first
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, defaultUri)
            val title = ringtone?.getTitle(context)?.let { "$it (Default)" } ?: "Default Alarm"
            title to defaultUri
        } catch (e: Exception) {
            Timber.e(e, "Failed to get system default alarm ringtone")
            try {
                // Fallback to notification sound
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val fallbackRingtone = RingtoneManager.getRingtone(context, fallbackUri)
                val title = fallbackRingtone?.getTitle(context)?.let { "$it (Default)" } ?: "Default Notification"
                title to fallbackUri
            } catch (e: Exception) {
                Timber.e(e, "Failed to get fallback notification sound")
                // Final fallback to any ringtone
                "Default Ringtone" to RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
        }
    }

    /**
     * Plays a ringtone for preview duration.
     * Handles stopping current playback and managing playback state.
     *
     * @param uri URI of the ringtone to play, null to stop playback
     */
    override suspend fun play(uri: Uri?) {
        try {
            // Toggle playback if same ringtone
            if (uri == currentUri && isPlaying) {
                stopPlaying()
                return
            }

            stopPlaying()

            uri?.let {
                val ringtone = RingtoneManager.getRingtone(context, uri)?.also { newRingtone ->
                    currentRingtone = newRingtone
                    currentUri = uri
                    isPlaying = true
                    newRingtone.play()
                } ?: return

                // Play for preview duration
                delay(PLAY_DURATION_MS)

                // Stop if still the active playback
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

    /**
     * Stops current ringtone playback and cleans up state.
     */
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
        private const val PLAY_DURATION_MS = 5000L  // 5 second preview
    }
}