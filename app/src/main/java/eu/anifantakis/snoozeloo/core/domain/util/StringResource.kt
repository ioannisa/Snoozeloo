package eu.anifantakis.snoozeloo.core.domain.util

import androidx.annotation.StringRes

data class ResourceString(
    @StringRes val id: Int,
    val args: Array<Any> = arrayOf()
)