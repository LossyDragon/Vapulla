package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class PersonaStateBuffer(
    private val steamFriendDao: SteamFriendDao,
    private val scope: CoroutineScope,
) {
    private val map: MutableMap<SteamID, PersonaStateCallback> = ConcurrentHashMap()

    private val processingMutex = Mutex()

    private val shutdownChannel = Channel<Unit>(Channel.UNLIMITED)

    private var processingJob: Job? = null

    fun push(state: PersonaStateCallback) {
        val existingState = map[state.friendId]

        if (existingState == null ||
            state.personaState != EPersonaState.Offline ||
            state.lastLogoff > existingState.lastLogoff
        ) {
            map[state.friendId] = state
        }
    }

    private suspend fun process() {
        if (map.isEmpty()) return

        val friendsToUpdate = mutableListOf<SteamFriend>()

        val statesToProcess = processingMutex.withLock {
            val snapshot = map.toMap()
            map.clear()
            snapshot
        }

        statesToProcess.forEach { (id, state) ->
            val friend = steamFriendDao.find(id.convertToUInt64())


            if (friend != null && shouldUpdateFriend(friend, state)) {
                updateFriendFromState(friend, state)
                friendsToUpdate.add(friend)
            }
        }

        if (friendsToUpdate.isNotEmpty()) {
            steamFriendDao.insert(friendsToUpdate)
        }
    }

    private fun shouldUpdateFriend(friend: SteamFriend, state: PersonaStateCallback): Boolean {
        return state.personaState != EPersonaState.Offline ||
                state.lastLogoff.time > friend.lastLogOff
    }

    private fun updateFriendFromState(friend: SteamFriend, state: PersonaStateCallback) {
        val avatarHash = state.avatarHash.toHexString()
        friend.name = state.playerName
        friend.avatar = avatarHash
        friend.state = state.personaState
        // friend.gameName = steamAppDao.findApp(state.gamePlayedAppId)?.name ?: state.gameName
        friend.gameAppID = state.gamePlayedAppId
        friend.lastLogOn = state.lastLogon.time
        friend.lastLogOff = state.lastLogoff.time
        friend.stateFlags = state.personaStateFlags
    }

    fun start() {
        if (processingJob?.isActive == true) {
            Timber.i("PersonaStateBuffer is already running")
            return
        }

        processingJob = scope.launch {
            Timber.i("Starting persona state buffer")

            try {
                flow {
                    while (currentCoroutineContext().isActive) {
                        emit(Unit)
                        delay(1000L)
                    }
                }
                    .takeUntil(shutdownChannel.receiveAsFlow())
                    .collect {
                    try {
                        process()
                    } catch (e: Exception) {
                        Timber.i("Error during persona state processing: ${e.message}")
                    }
                }
            } finally {
                Timber.i("Stopping persona state buffer")
            }
        }
    }

    suspend fun stop() {
        shutdownChannel.trySend(Unit)
        processingJob?.join()
        processingJob = null
    }

    fun cleanup() {
        shutdownChannel.close()
    }
}

private fun <T> Flow<T>.takeUntil(other: Flow<*>): Flow<T> = flow {
    coroutineScope {
        val stopSignal = async {
            other.first()
        }

        this@takeUntil.collect { value ->
            if (stopSignal.isCompleted) return@collect
            emit(value)
        }
    }
}