package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import java.util.LinkedList
import org.spongycastle.util.encoders.Hex
import timber.log.Timber

class PersonaStateBuffer(val steamFriendDao: SteamFriendDao) {
    private val map: MutableMap<SteamID, PersonaState> = hashMapOf()

    private val mapLock: Any = Any()

    @Volatile
    private var isRunning = false

    private val thread: Runnable = Runnable {
        Timber.i("starting persona state buffer thread")
        isRunning = true

        while (isRunning) {
            Thread.sleep(1000L)
            process()
        }

        Timber.i("stopping persona state buffer thread")
    }

    fun push(state: PersonaState) {
        synchronized(mapLock) {
            val old = map[state.friendID]

            if (old == null || state != old) {
                map[state.friendID] = state
            }
        }
    }

    private fun process() {
        val friendsToUpdate: MutableList<SteamFriend> = LinkedList()

        synchronized(mapLock) {
            if (map.isEmpty()) {
                return
            }

            map.forEach { (id, state) ->
                steamFriendDao.find(id.convertToUInt64())?.let { friend ->
                    val oldFriend = friend.copy()

                    if (state.avatarHash.isNotEmpty()) {
                        friend.avatar = Hex.toHexString(state.avatarHash)
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
        Thread(thread, "PersonaStateBufferThread").start()
    }

    fun stop() {
        isRunning = false
    }
}
