package eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting

import android.net.Uri

data class RingtoneState(
    val ringtones: List<Pair<String, Uri>> = emptyList(),
    val selectedRingtone: Pair<String, Uri>? = null
)