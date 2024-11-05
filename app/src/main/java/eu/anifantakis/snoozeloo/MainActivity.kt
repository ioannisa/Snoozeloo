package eu.anifantakis.snoozeloo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppActionButton
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppBackground
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppOutlinedActionButton
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.chips.AppWeeklyChips
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.state.showSplash
            }
        }

        enableEdgeToEdge()
        setContent {
            SnoozelooTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .padding(UIConst.padding),
        verticalArrangement = Arrangement.spacedBy(UIConst.paddingSmall)
    ) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )

        AppActionButton(
            text = "TEST 1"
        ) { }

        AppOutlinedActionButton(
            text = "TEST 2"
        ) { }

        AppWeeklyChips(
            onSelectionChanged = { selectedDays ->

            }
        )
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SnoozelooTheme {
        AppBackground {
            Greeting("Android")
        }
    }
}