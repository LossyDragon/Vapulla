package `in`.dragonbra.vapulla.compose.screens.home

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import timber.log.Timber

class HomeActivity : VapullaBaseActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("HomeActivity")
        setContent {
            VapullaTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

}