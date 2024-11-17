package eu.anifantakis.snoozeloo.alarm.domain

import android.net.Uri

interface RingtoneRepository {
    fun getAllRingtones(): List<Pair<String, Uri>>
    fun getSystemDefaultAlarmRingtone(): Pair<String, Uri>
    suspend fun play(uri: Uri?)
    suspend fun stopPlaying()
}