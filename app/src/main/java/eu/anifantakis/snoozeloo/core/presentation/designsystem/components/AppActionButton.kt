package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme

@Composable
fun AppActionButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    largeText: Boolean = true,
    icon: ImageVector? = null,
    cornerRadius: Dp = 30.dp,
    fillWidth: Boolean = true,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = UIConst.colorWithAlpha(Color.Gray, 0.3f),
            disabledContentColor = UIConst.colorWithAlpha(MaterialTheme.colorScheme.onPrimary)
        ),
        shape = RoundedCornerShape(cornerRadius),
        modifier = modifier.defaultMinSize(
            minWidth = 42.dp,  // Override Material default min width
            minHeight = 42.dp  // Override Material default min height
        ),

        contentPadding =  contentPadding
    ) {
        Box(
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)

        ) {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp)
                        .align(Alignment.Center)
                )
            }

            text?.let {
                if (largeText) {
                    AppText24(
                        text = text,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.W700,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                } else {
                    AppText16(
                        text = text,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.W700,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
            }


        }
    }
}


@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppOutlinedActionButtonPreview() {
    SnoozelooTheme {
        AppBackground {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("enabled: true")
                AppActionButton(
                    text = "Sign up",
                    enabled = true,
                    onClick = {}
                )

                Text("enabled: false")
                AppActionButton(
                    text = "Register",
                    enabled = false,
                    onClick = {}
                )

                AppActionButton(
                    icon = Icons.close,
                    cornerRadius = 10.dp,
                    fillWidth = false,
                    contentPadding = PaddingValues(all = 0.dp)
                ) { }

                AppActionButton(
                    text = "Save",
                    largeText = false,
                    cornerRadius = 24.dp,
                    fillWidth = false,
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) { }
            }
        }
    }
}