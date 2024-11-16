package eu.anifantakis.snoozeloo.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AlarmEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val hour: Int = 0,
    val minute: Int = 0,
    val enabled: Boolean = false,
    val title: String = "",
    val ringtoneTitle: String = "",
    val ringtoneUri: String? = null,
    val volume: Float = 0.5f,
    val vibrate: Boolean = true,
    val mo: Boolean = false,
    val tu: Boolean = false,
    val we: Boolean = false,
    val th: Boolean = false,
    val fr: Boolean = false,
    val sa: Boolean = false,
    val su: Boolean = false,
)