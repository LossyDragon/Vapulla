package `in`.dragonbra.vapulla.ui.screens.sessions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.AuthSession
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class SessionsUiState(
    val sessions: ImmutableList<AuthSession> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** Devices signed in to the account, via the `Authentication` service. */
class SessionsViewModel(private val connection: ServiceConnection) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            Timber.d("Refreshing sessions")
            _uiState.update { it.copy(isLoading = true, error = null) }
            val apis = connection.steamService?.apis
            if (apis == null) {
                _uiState.update { it.copy(isLoading = false, error = "Not connected to Steam") }
                return@launch
            }

            runCatching { apis.sessions.sessions() }
                .onSuccess { sessions ->
                    _uiState.update {
                        it.copy(sessions = sessions.toImmutableList(), isLoading = false)
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "EnumerateTokens failed")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error =
                                error.message ?: "Request failed",
                        )
                    }
                }
        }
    }

    fun revoke(session: AuthSession) {
        viewModelScope.launch {
            val apis = connection.steamService?.apis ?: return@launch
            val result = runCatching { apis.sessions.revoke(session.tokenId) }
                .onFailure { Timber.w(it, "RevokeRefreshToken failed") }
                .getOrElse { EResult.Fail }

            if (result == EResult.OK) {
                _messages.emit("Signed out ${session.description.ifBlank { session.platform }}")
                refresh()
            } else {
                _messages.emit("Steam answered $result")
            }
        }
    }
}
