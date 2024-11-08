package eu.anifantakis.snoozeloo.alarm.domain

import android.net.Uri

interface RingtoneRepository {
    fun getDefaultRingtones(): List<Pair<String, Uri>>
    suspend fun play(uri: Uri)
}