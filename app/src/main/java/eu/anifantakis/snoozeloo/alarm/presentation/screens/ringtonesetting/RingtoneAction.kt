package eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting

import android.net.Uri

sealed interface RingtoneAction {
    data object OnOpenRingtonesSetting : RingtoneAction
    data class OnSelectRingtone(val ringtone: Pair<String, Uri>?) : RingtoneAction
    // todo: DO NOT navigate back when the user selects a new option
    data object OnNavigateBack : RingtoneAction
}