package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import timber.log.Timber
import java.util.*

class PersonaStateBuffer(val steamFriendDao: SteamFriendDao) {
    private val map: MutableMap<SteamID, PersonaStateCallback> = hashMapOf()

    private val mapLock: Any = Any()

    @Volatile
    private var isRunning = false

    private val thread: Runnable = Runnable {
        Timber.i("Starting persona state buffer thread.")
        isRunning = true

        while (isRunning) {
            Thread.sleep(1000L)
            process()
        }

        Timber.i("stopping persona state buffer thread")
    }

    fun push(state: PersonaStateCallback) {
        synchronized(mapLock) {
            if (map.contains(state.friendID)) {
                val old = map[state.friendID]

                if (state.state != EPersonaState.Offline || state.lastLogOff > old?.lastLogOff) {
                    map[state.friendID] = state
                }
            } else {
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

            map.entries.forEach {
                val id = it.key
                val state = it.value

                val friend = steamFriendDao.find(id.convertToUInt64())

                if (friend != null && (state.state != EPersonaState.Offline || state.lastLogOff.time > friend.lastLogOff)) {
                    val avatarHash = state.avatarHash.toHexString()

                    friend.name = state.name
                    friend.avatar = avatarHash
                    friend.state = state.state
                    friend.gameName = state.gameName
                    friend.gameAppId = state.gameAppID
                    friend.lastLogOn = state.lastLogOn.time
                    friend.lastLogOff = state.lastLogOff.time
                    friend.stateFlags = state.stateFlags

                    friendsToUpdate.add(friend)
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