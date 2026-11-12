package `in`.dragonbra.vapulla.service.sync

import android.content.Context
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendsListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.NicknameListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.db.VapullaDatabase
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.SessionComponent
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.helpers.toLong
import java.io.Closeable
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Keeps the friends table in step with Steam: who is on the list, their profile and presence,
 * nicknames, and the name of the game each one is playing.
 *
 * @param scope outlives a single sign-in; used for work started by a callback.
 * @param licenses asked for the app info of games whose name is not known yet; it answers
 *  through [nameGames].
 */
class FriendsSync(
    private val context: Context,
    private val scope: CoroutineScope,
    private val db: VapullaDatabase,
    private val account: AccountManager,
    private val localSteamId: () -> SteamID?,
    private val licenses: LicenseSync,
) : SessionComponent {

    /** Serialises every writer of the friends table's presence and membership. */
    private val friendsListMutex = Mutex()

    /** Incoming friend requests waiting for a name and avatar before they are notified. */
    private val requestsToNotify = mutableSetOf<SteamID>()

    override fun subscribe(callbacks: CallbackManager): List<Closeable> = listOf(
        callbacks.subscribe(onPersonaState),
        callbacks.subscribe(onFriendsList),
        callbacks.subscribe(onNicknameList),
    )

    override fun start(session: CoroutineScope) {
        session.continuousFriendChecker()
    }

    /**
     * Sets every stored friend offline. Presence stored from the previous session is stale,
     * and Steam only pushes state for friends who are online now, so everyone must start out
     * offline. Called at logon, this queues on [friendsListMutex] well before the first
     * persona push, which follows the friends list and a network round trip.
     */
    fun resetPresence() {
        scope.launch {
            friendsListMutex.withLock {
                val dao = db.steamFriendDao()
                val reset = dao.findWithPresence().map { friend ->
                    val offline = SteamFriend(id = friend.id)
                    friend.copy(
                        state = offline.state,
                        gameAppID = offline.gameAppID,
                        gameID = offline.gameID,
                        gameDataBlob = offline.gameDataBlob,
                        gameName = offline.gameName,
                        stateFlags = offline.stateFlags,
                    )
                }
                Timber.d("resetPresence: resetting stored presence of %d friends", reset.size)
                dao.update(reset)
            }
        }
    }

    /**
     * Fills in the game name of friends playing one of the apps in [names] (app id to name).
     * They were stored without one because the app was unknown when their presence arrived.
     */
    suspend fun nameGames(names: Map<Int, String>) {
        if (names.isEmpty()) {
            return
        }

        friendsListMutex.withLock {
            val friendDao = db.steamFriendDao()
            val renamed = friendDao.findFriendsInGame().mapNotNull { friend ->
                names[friend.gameAppID]
                    ?.takeIf { name -> name != friend.gameName }
                    ?.let { name -> friend.copy(gameName = name) }
            }
            if (renamed.isNotEmpty()) {
                Timber.d("Named the game of ${renamed.size} friend(s) from PICS")
                friendDao.update(renamed)
            }
        }
    }

    private val onPersonaState: Consumer<PersonaStateCallback> = Consumer {
        // After the friends list arrives, JavaSteam sends ClientRequestFriendData for every
        // friend. Steam answers with profile-only messages in which persona_state and the game
        // fields are absent on the wire. Real presence for online friends arrives in separate
        // pushes, in no fixed order relative to those replies, so a reply must never overwrite
        // a stored state.
        val profileOnly = !it.hasPersonaState

        Timber.d(
            "onPersonaState() id=%d name='%s' state=%s game=%d profileOnly=%b",
            it.friendId.toLong(),
            it.playerName,
            it.personaState,
            it.gamePlayedAppId,
            profileOnly,
        )

        if (!it.friendId.isIndividualAccount) {
            return@Consumer
        }

        if (it.friendId == localSteamId()) {
            Timber.d("Updating local user info")
            scope.launch {
                account.saveLocalUser(it)
            }
            return@Consumer
        }

        scope.launch {
            friendsListMutex.withLock {
                val dao = db.steamFriendDao()
                val dbFriend = dao.find(it.friendId.toLong())

                if (dbFriend == null) {
                    Timber.d("Persona ${it.playerName} not in db to update persona state.")
                    return@launch
                }

                val withProfile = dbFriend.copy(
                    name = it.playerName,
                    avatar = it.avatarHash.toHexString(),
                    lastLogOn = it.lastLogon,
                    lastLogOff = it.lastLogoff,
                    statusFlags = it.statusFlags,
                )

                val updatedFriend = if (profileOnly) {
                    withProfile
                } else {
                    withProfile.copy(
                        state = it.personaState,
                        gameAppID = it.gamePlayedAppId,
                        gameID = it.gameId,
                        gameDataBlob = it.gameDataBlob,
                        richPresence = it.richPresence.associate { kv -> kv.key to kv.value },
                        gameName = it.gameName.ifEmpty {
                            if (it.gamePlayedAppId > 0) {
                                db.steamAppDao().findAppName(it.gamePlayedAppId) ?: ""
                            } else {
                                ""
                            }
                        },
                        stateFlags = it.personaStateFlags,
                    )
                }

                // A game Steam did not name and the database does not know yet: ask for its
                // app info straight away. nameGames() fills the name in when it arrives.
                if (!profileOnly && it.gamePlayedAppId > 0 && updatedFriend.gameName.isEmpty()) {
                    licenses.requestAppInfo(it.gamePlayedAppId)
                }

                // Skip the write when nothing changed, otherwise every persona tick
                // invalidates the friends flow and re-runs the whole home pipeline.
                if (updatedFriend != dbFriend) {
                    if (updatedFriend.state != dbFriend.state) {
                        Timber.d(
                            "onPersonaState: %d '%s' %s -> %s",
                            dbFriend.id,
                            updatedFriend.name,
                            dbFriend.state,
                            updatedFriend.state,
                        )
                    }
                    dao.update(updatedFriend)
                }

                if (requestsToNotify.contains(it.friendId)) {
                    NotificationHelper.sendFriendRequestNotification(
                        context = context,
                        friendId = it.friendId.toLong(),
                        friendName = it.playerName,
                        avatarUrl = Utils.getAvatarURL(it.avatarHash.toHexString()),
                    )
                    requestsToNotify.remove(it.friendId)
                }
            }
        }
    }

    private val onFriendsList: Consumer<FriendsListCallback> = Consumer {
        Timber.d("onFriendsList()")
        val inc = it.isIncremental

        scope.launch {
            friendsListMutex.withLock {
                val dao = db.steamFriendDao()
                val friendsToAdd = mutableListOf<SteamFriend>()
                val friendsToUpdate = mutableListOf<SteamFriend>()
                val friendsToRemove = mutableListOf<SteamFriend>()

                it.friendList.forEach { friendItem ->
                    if (!friendItem.steamID.isIndividualAccount) {
                        return@forEach
                    }

                    var friend = dao.find(friendItem.steamID.toLong())

                    if (friend == null) {
                        if (friendItem.relationship == EFriendRelationship.Friend ||
                            friendItem.relationship == EFriendRelationship.RequestRecipient
                        ) {
                            val newFriend = SteamFriend(
                                id = friendItem.steamID.toLong(),
                                relation = friendItem.relationship,
                            )
                            friendsToAdd.add(newFriend)
                        }
                    } else {
                        if (friendItem.relationship == EFriendRelationship.Friend ||
                            friendItem.relationship == EFriendRelationship.RequestRecipient
                        ) {
                            val updatedFriend = friend.copy(relation = friendItem.relationship)
                            friendsToUpdate.add(updatedFriend)
                        } else {
                            friendsToRemove.add(friend)
                        }
                    }

                    if (inc && friendItem.relationship == EFriendRelationship.RequestRecipient) {
                        requestsToNotify.add(friendItem.steamID)
                    }
                }

                Timber.d(
                    "onFriendsList: incremental=%b received=%d add=%d update=%d remove=%d",
                    inc,
                    it.friendList.size,
                    friendsToAdd.size,
                    friendsToUpdate.size,
                    friendsToRemove.size,
                )
                dao.insert(friendsToAdd)
                dao.update(friendsToUpdate)
                dao.remove(friendsToRemove)
            }
        }
    }

    private val onNicknameList: Consumer<NicknameListCallback> = Consumer {
        Timber.d("onNicknameList()")
        scope.launch {
            val dao = db.steamFriendDao()
            val nicknames = it.nicknames.associate { info ->
                info.steamID.toLong() to info.nickname
            }

            // Only clear rows losing their nickname; rows keeping one are set below.
            dao.clearNicknamesExcept(nicknames.keys.toList())

            val friendsToUpdate = nicknames.mapNotNull { (id, nickname) ->
                dao.find(id)
                    ?.takeIf { friend -> friend.nickname != nickname }
                    ?.copy(nickname = nickname)
            }

            dao.update(friendsToUpdate)
        }
    }

    /** Safety net for game names: re-checks in-game friends against the apps table. */
    private fun CoroutineScope.continuousFriendChecker() = launch {
        val appDao = db.steamAppDao()
        val friendDao = db.steamFriendDao()
        while (isActive) {
            delay(15.seconds)
            friendDao.findFriendsInGame()
                .also { friends -> Timber.d("Found ${friends.size} friends in game") }
                .forEach { friend ->
                    if (friend.gameAppID <= 0) {
                        return@forEach
                    }

                    appDao.findAppName(friend.gameAppID)?.let { name ->
                        if (friend.gameName != name) {
                            friendDao.update(friend.copy(gameName = name))
                        }
                    } ?: licenses.requestAppInfo(friend.gameAppID)
                }
        }
    }
}
