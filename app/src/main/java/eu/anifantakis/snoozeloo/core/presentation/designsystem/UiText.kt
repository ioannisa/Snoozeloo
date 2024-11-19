package eu.anifantakis.snoozeloo.core.presentation.designsystem

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {

    data class DynamicString(val value: String): UiText

    class StringResource(
        @StringRes val id: Int,
        vararg val args: Array<Any> = arrayOf()
    ): UiText {
        constructor(stringResource: Pair<Int, Array<Any>>): this(
            id = stringResource.first,
            args = arrayOf(stringResource.second)
        )

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