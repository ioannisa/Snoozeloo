package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme

@Composable
fun AppOutlinedActionButton(
    text: String,
    largeText: Boolean = true,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = UIConst.grayOutColor(MaterialTheme.colorScheme.primary),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if(enabled) MaterialTheme.colorScheme.primary else UIConst.grayOutColor(
                MaterialTheme.colorScheme.primary)
        ),

        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(UIConst.colorWithAlpha(MaterialTheme.colorScheme.primary)),

        contentPadding =  contentPadding
    ) {
        Box(
            modifier = Modifier

                .fillMaxWidth()
                .padding(vertical = 8.dp)

        ) {
            if (largeText) {
                AppText24(
                    text = text,
                    fontWeight = FontWeight.W700,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            } else {
                AppText16(
                    text = text,
                    fontWeight = FontWeight.W700,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
        }
    }
}


@Preview
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
                AppOutlinedActionButton(
                    text = "Sign up",
                    enabled = true,
                    onClick = {}
                )

                Text("enabled: false")
                AppOutlinedActionButton(
                    text = "Register",
                    enabled = false,
                    onClick = {}
                )


                AppOutlinedActionButton(
                    text = "Register",
                    largeText = false,
                    contentPadding = PaddingValues(0.dp),
                    enabled = false,
                    onClick = {}
                )
            }
        }
    }
}