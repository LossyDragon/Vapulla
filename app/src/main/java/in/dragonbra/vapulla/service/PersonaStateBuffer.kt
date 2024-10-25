package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStatesCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import timber.log.Timber

class PersonaStateBuffer(val steamFriendDao: SteamFriendDao) {
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val map: MutableMap<SteamID, PersonaStatesCallback> = hashMapOf()
    private val mapLock: Any = Any()

    fun push(state: PersonaStatesCallback) {
        synchronized(mapLock) {
            val old = map[state.friendID]

            if (old == null || state != old) {
                map[state.friendID] = state
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun process() {
        val friendsToUpdate: MutableList<SteamFriend> = LinkedList()

        synchronized(mapLock) {
            if (map.isEmpty()) {
                return
            }

            Timber.d("Processing friends: ${map.size}")

            map.forEach { (id, state) ->
                steamFriendDao.find(id.convertToUInt64())?.let { friend ->
                    val oldFriend = friend.copy()

                    if (state.avatarHash.isNotEmpty()) {
                        friend.avatar = state.avatarHash.toHexString()
                    }

                    if (state.name.isNotEmpty()) {
                        friend.name = state.name
                    }

                    if (state.lastLogOff.time > 0) {
                        friend.lastLogOff = state.lastLogOff.time
                    }

                    if (state.lastLogOn.time > 0) {
                        friend.lastLogOn = state.lastLogOn.time
                    }

                    friend.gameAppId = state.gameAppID
                    friend.gameName = state.gameName
                    friend.state = state.state.code()
                    friend.stateFlags = EPersonaStateFlag.code(state.stateFlags)

                    if (friend != oldFriend) {
                        friendsToUpdate.add(friend)
                    }
                }
            }

            map.clear()
        }

        steamFriendDao.insert(*friendsToUpdate.toTypedArray())
    }

    fun start() {
        executorService.submit {
            Timber.i("starting persona state buffer thread")
            try {
                while (!Thread.currentThread().isInterrupted) {
                    Thread.sleep(1000L)
                    process()
                }
            } catch (e: InterruptedException) {
                Timber.i("Thread was interrupted")
            } finally {
                Timber.i("stopping persona state buffer thread")
            }
        }
    }

    fun stop() {
        executorService.shutdownNow() // Initiates an immediate shutdown
        try {
            if (!executorService.awaitTermination(1000, TimeUnit.SECONDS)) {
                Timber.w("Executor did not terminate in the specified time.")
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
    }
}
