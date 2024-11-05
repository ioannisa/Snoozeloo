package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme

@Composable
fun AppActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = UIConst.grayOutColor(MaterialTheme.colorScheme.primary),
            disabledContentColor = UIConst.grayOutColor(MaterialTheme.colorScheme.onPrimary)
        ),
        shape = RoundedCornerShape(30.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = UIConst.paddingSmall)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .align(Alignment.Center)
            )
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
            }
        }
    }
}