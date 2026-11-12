package `in`.dragonbra.vapulla.ui.screens.cloud

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.CloudFile
import java.io.File
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Immutable
data class CloudFilesUiState(
    val files: ImmutableList<CloudFile> = persistentListOf(),
    val isLoading: Boolean = true,
    val downloading: String? = null,
    val error: String? = null,
)

/**
 * Steam Cloud files for one app. Downloads land in the app's external files directory under
 * `cloud/<appId>/`, which needs no storage permission.
 */
class CloudFilesViewModel(
    private val appId: Int,
    private val connection: ServiceConnection,
    private val application: Application,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudFilesUiState())
    val uiState: StateFlow<CloudFilesUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            Timber.d("Refreshing cloud files for app $appId")
            _uiState.update { it.copy(isLoading = true, error = null) }
            val apis = connection.steamService?.apis
            if (apis == null) {
                _uiState.update { it.copy(isLoading = false, error = "Not connected to Steam") }
                return@launch
            }

            runCatching { apis.cloud.files(appId) }
                .onSuccess { files ->
                    _uiState.update {
                        it.copy(
                            files = files.sortedBy { f ->
                                f.filename.lowercase()
                            }.toImmutableList(),
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "EnumerateUserFiles failed")
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

    fun download(file: CloudFile) {
        if (_uiState.value.downloading != null) return

        viewModelScope.launch {
            val apis = connection.steamService?.apis ?: return@launch
            _uiState.update { it.copy(downloading = file.filename) }

            val result = runCatching {
                val bytes = apis.cloud.download(appId, file.filename)
                withContext(Dispatchers.IO) {
                    val target =
                        File(application.getExternalFilesDir(null), "cloud/$appId/${file.filename}")
                    target.parentFile?.mkdirs()
                    target.writeBytes(bytes)
                    target
                }
            }

            _uiState.update { it.copy(downloading = null) }
            _messages.emit(
                result.fold(
                    onSuccess = { "Saved to ${it.absolutePath}" },
                    onFailure = {
                        Timber.w(it, "Cloud download failed")
                        it.message ?: "Download failed"
                    },
                ),
            )
        }
    }
}
