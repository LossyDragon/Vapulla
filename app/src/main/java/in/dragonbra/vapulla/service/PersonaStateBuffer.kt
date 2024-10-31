package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStatesCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

class PersonaStateBuffer(private val steamFriendDao: SteamFriendDao) {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
            Timber.e(e, "Error in PersonaStateBuffer")
        }
    )

    private val _stateFlow = MutableStateFlow<Map<SteamID, PersonaStatesCallback>>(emptyMap())
    private val bufferJob = Job()

    fun push(state: PersonaStatesCallback) {
        _stateFlow.update { currentMap ->
            if (currentMap[state.friendID] != state) {
                currentMap + (state.friendID to state)
            } else {
                currentMap
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun processStates(states: Map<SteamID, PersonaStatesCallback>) {
        if (states.isEmpty()) return

        val friendsToUpdate = withContext(Dispatchers.IO) {
            states.mapNotNull { (id, state) ->
                steamFriendDao.find(id.convertToUInt64())?.let { friend ->
                    val oldFriend = friend.copy()

                    friend.copy(
                        avatar = if (state.avatarHash.isNotEmpty()) state.avatarHash.toHexString() else friend.avatar,
                        name = state.name.ifEmpty { friend.name },
                        lastLogOff = if (state.lastLogOff.time > 0) state.lastLogOff.time else friend.lastLogOff,
                        lastLogOn = if (state.lastLogOn.time > 0) state.lastLogOn.time else friend.lastLogOn,
                        gameAppId = state.gameAppID,
                        gameName = state.gameName,
                        state = state.state.code(),
                        stateFlags = EPersonaStateFlag.code(state.stateFlags)
                    ).takeIf { it != oldFriend }
                }
            }
        }

        if (friendsToUpdate.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                steamFriendDao.updateAll(friendsToUpdate)
            }
        }

        _stateFlow.update { emptyMap() }
    }

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch(bufferJob) {
            Timber.i("Starting persona state buffer")
            try {
                _stateFlow
                    .sample(1000L)
                    .collect(::processStates)
            } catch (e: CancellationException) {
                Timber.i("Persona state buffer was cancelled")
            } finally {
                Timber.i("Stopping persona state buffer")
            }
        }
    }

    fun stop() {
        scope.launch {
            bufferJob.cancelAndJoin()
            scope.cancel()
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}
