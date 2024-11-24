package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import eu.anifantakis.snoozeloo.ui.theme.bodyFontFamily

private val textStyle = TextStyle(
    fontFamily = bodyFontFamily,
    fontSize = 52.sp,
    lineHeight = 63.39.sp,
    fontWeight = FontWeight.W500,
)


@Composable
fun AppText12(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.W500,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyle.copy(
            fontSize = 12.sp,
            fontWeight = fontWeight,
            lineHeight = 16.sp,
            color = color
        ),
    )
}

@Composable
fun AppText14(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.W600,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyle.copy(
            fontSize = 14.sp,
            fontWeight = fontWeight,
            lineHeight = 17.07.sp,
            color = color
        ),
    )
}

@Composable
fun AppText16(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.W500,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyle.copy(
            fontSize = 16.sp,
            fontWeight = fontWeight,
            lineHeight = 19.5.sp,
            color = color
        ),
    )
}

@Composable
fun AppText20(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.W500,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyle.copy(
            fontSize = 20.sp,
            fontWeight = fontWeight,
            lineHeight = 25.26.sp,
            color = color
        ),
    )
}

@Composable
fun AppText24(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.W500,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyle.copy(
            fontSize = 24.sp,
            fontWeight = fontWeight,
            lineHeight = 29.26.sp,
            color = color
        ),
    )
}

@Composable
fun AppText42(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.W500,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyle.copy(
            fontSize = 42.sp,
            fontWeight = fontWeight,
            lineHeight = 51.2.sp,
            color = color
        ),
    )
}

@Composable
fun AppText52(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.W700,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyle.copy(
            fontSize = 52.sp,
            lineHeight = 63.39.sp,
            fontWeight = fontWeight,
            color = color
        ),
    )
}

@Composable
fun AppText82(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.W700,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyle.copy(
            fontSize = 82.sp,
            lineHeight = 103.39.sp,
            fontWeight = fontWeight,
            color = color
        ),
    )
}



@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppTextPreview() {
    SnoozelooTheme {
        AppBackground {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(UIConst.paddingSmall)
            ) {
                AppText12("This is AppText 12")
                AppText14("This is AppText 14")
                AppText16("This is AppText 16")
                AppText24("This is AppText 24")
                AppText42("This is AppText 42")
                AppText52("This is AppText 52")
                AppText82("This is AppText 82")
            }
        }
    }
}