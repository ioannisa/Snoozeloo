package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme

@Composable
fun AppScreenWithFAB(
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Main content
        content()

        // Centered FAB
        FloatingActionButton(
            onClick = onFabClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = UIConst.paddingDouble),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.add,
                contentDescription = "Add",
                modifier = Modifier
                    .width(UIConst.paddingDouble)
                    .height(UIConst.paddingDouble)
            )
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppScreenWithFABPreview() {
    SnoozelooTheme {
        AppBackground(
            modifier = Modifier.fillMaxSize()
        ) {
            AppScreenWithFAB(
                {}
            ) {

            }
        }
    }
}