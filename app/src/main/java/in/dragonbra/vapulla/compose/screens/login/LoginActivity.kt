package `in`.dragonbra.vapulla.compose.screens.login

import android.os.Bundle
import androidx.activity.compose.setContent
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import timber.log.Timber

class LoginActivity : VapullaBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("LoginActivity")
        setContent {
            VapullaTheme {
                LoginScreen()
            }
        }
    }
}