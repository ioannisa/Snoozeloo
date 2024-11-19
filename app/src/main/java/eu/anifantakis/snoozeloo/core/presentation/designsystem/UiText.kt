package eu.anifantakis.snoozeloo.core.presentation.designsystem

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.anifantakis.snoozeloo.core.domain.util.ResourceString

sealed interface UiText {

    data class DynamicString(val value: String): UiText

    class StringResource(
        @StringRes val id: Int,
        vararg val args: Array<Any> = arrayOf()
    ): UiText {
        fun flattenArgs(): Array<Any> = args.flatMap { it.toList() }.toTypedArray()
    }

    @Composable
    fun asString(): String {
        return when(this) {
            is DynamicString -> value
            is StringResource -> stringResource(id, *flattenArgs())
        }
    }

    fun asString(context: Context): String {
        return when(this) {
            is DynamicString -> value
            is StringResource -> context.getString(id, *flattenArgs())
        }
    }
}

fun ResourceString.toUiText(): UiText.StringResource {
    return UiText.StringResource(id, args)
}