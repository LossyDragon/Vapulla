package `in`.dragonbra.vapulla.compose.screens.login

import android.content.ServiceConnection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val serviceConnection: ServiceConnection
) : ViewModel() {

    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var steamGuard by mutableStateOf("")
    var isPasswordVisible by mutableStateOf(false)
    var isSteamGuardVisible by mutableStateOf(false)
        private set


}