package `in`.dragonbra.vapulla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ramcosta.composedestinations.DestinationsNavHost
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.vapulla.compose.screens.login.NavGraphs
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VapullaTheme {
                DestinationsNavHost(navGraph = NavGraphs.root)
            }
        }
    }
}