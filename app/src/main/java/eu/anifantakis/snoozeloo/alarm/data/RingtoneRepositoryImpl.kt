package eu.anifantakis.snoozeloo.alarm.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import eu.anifantakis.snoozeloo.alarm.domain.RingtoneRepository
import kotlinx.coroutines.delay

class RingtoneRepositoryImpl(private val context: Context) : RingtoneRepository {

    override fun getDefaultRingtones(): List<Pair<String, Uri>> {
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)
        val cursor = ringtoneManager.cursor
        val ringtones = mutableListOf<Pair<String, Uri>>()

        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = ringtoneManager.getRingtoneUri(cursor.position)
            ringtones.add(title to uri)
        }

        cursor.close()
        return ringtones
    }

    override suspend fun play(uri: Uri) {
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone.play()
        delay(5000)
        if (ringtone.isPlaying) {
            ringtone.stop()
        }
    }

}