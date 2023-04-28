package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import java.util.*
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
            if (map.contains(state.friendID)) {
                val old = map[state.friendID]

                if (state.state != EPersonaState.Offline || state.lastLogOff > old?.lastLogOff) {
                    map[state.friendID] = state
                }
                return
            }

            map[state.friendID] = state
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
                    val isOnline = state.state != EPersonaState.Offline
                    val isTime = state.lastLogOff.time > friend.lastLogOff
                    if (isOnline || isTime) {
                        val avatarHash = Hex.toHexString(state.avatarHash)

                        friend.name = state.name
                        friend.avatar = avatarHash
                        friend.state = state.state.code()
                        friend.gameName = state.gameName
                        friend.gameAppId = state.gameAppID
                        friend.lastLogOn = state.lastLogOn.time
                        friend.lastLogOff = state.lastLogOff.time
                        friend.stateFlags = EPersonaStateFlag.code(state.stateFlags)

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
