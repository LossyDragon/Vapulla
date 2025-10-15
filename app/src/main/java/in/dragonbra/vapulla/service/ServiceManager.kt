package `in`.dragonbra.vapulla.service

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ServiceManager(
    private val application: Application
) {
    // Simple state properties
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    // Commands from ViewModel to Service
    private val _commandChannel = MutableSharedFlow<ServiceCommand>()
    val commandChannel: SharedFlow<ServiceCommand> = _commandChannel.asSharedFlow()

    // Events from Service to ViewModel
    private val _loginResult = MutableSharedFlow<LoginResult>()
    val loginResult: SharedFlow<LoginResult> = _loginResult.asSharedFlow()

    // ViewModel methods
    suspend fun sendCommand(command: ServiceCommand) {
        _commandChannel.emit(command)
    }

    fun startServiceLazy() {
        if (!isServiceRunning.value) {
            val intent = Intent(application, SteamService::class.java)
            application.startForegroundService(intent)
        }
    }

    // Service methods
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    suspend fun emitLoginResult(result: LoginResult) {
        _loginResult.emit(result)
    }
}

sealed class ServiceCommand {
    object Start : ServiceCommand()
    object Stop : ServiceCommand()
    data class Login(
        val username: String,
        val password: String,
        val refreshToken: String
    ) : ServiceCommand()
}

sealed class LoginResult {
    object StandBy : LoginResult()
    object Loading : LoginResult()
    object Success : LoginResult()
    data class Error(var error: String) : LoginResult()
    data class RequiresQRCode(val qrCode: String) : LoginResult()
}