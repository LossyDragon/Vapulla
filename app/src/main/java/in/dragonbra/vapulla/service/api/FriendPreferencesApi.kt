package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetPerFriendPreferences_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_PerFriendPreferencesChanged_Notification
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_SetPerFriendPreferences_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.ENotificationSetting
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.PerFriendPreferences
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.rpc.service.PlayerClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.vapulla.service.SessionComponent
import `in`.dragonbra.vapulla.util.helpers.accountIdToSteamID
import `in`.dragonbra.vapulla.util.helpers.toLong
import `in`.dragonbra.vapulla.util.helpers.toSteamID
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/** Whether a chat message from a friend is to be notified of on a phone. */
enum class MobileNotifications {
    /** As the account's own notification settings say. */
    Default,
    Always,
    Never,
}

/**
 * What the account has set for single friends about notifications on a phone: the "Mobile
 * notifications" switches of Steam's own app. Held in memory only; Steam sends the
 * preferences at every sign-in. A friend who is not in [mobile] is at
 * [MobileNotifications.Default].
 */
object SteamFriendPreferences {

    private val _mobile =
        MutableStateFlow<ImmutableMap<Long, MobileNotifications>>(persistentMapOf())

    /** The friends with a setting of their own, by 64-bit Steam ID. */
    val mobile: StateFlow<ImmutableMap<Long, MobileNotifications>> = _mobile.asStateFlow()

    /** The setting for [friendId] as it is now. */
    fun mobile(friendId: Long): MobileNotifications =
        _mobile.value[friendId] ?: MobileNotifications.Default

    internal fun set(mobile: Map<Long, MobileNotifications>) {
        _mobile.value = mobile.toImmutableMap()
    }
}

/**
 * Per-friend preferences via the `Player` service: read at sign-in, kept up to date from
 * `PlayerClient.NotifyPerFriendPreferencesChanged`, which Steam sends when one is changed on
 * any device, and changed with `SetPerFriendPreferences`.
 *
 * Steam keeps more per friend than the app shows: what the desktop client pops up and
 * sounds for when the friend comes online, starts a game or writes. Only the notifications
 * on a phone are used here.
 */
class FriendPreferencesApi(private val player: Player) : SessionComponent {

    /**
     * Each friend's preferences as Steam sent them, by account ID. Changing one sends the
     * whole record back, so the rest of it is kept for that.
     */
    private val records = ConcurrentHashMap<Int, PerFriendPreferences>()

    override fun subscribe(callbacks: CallbackManager): List<Closeable> = listOf(
        callbacks.subscribeServiceNotification<
            PlayerClient,
            CPlayer_PerFriendPreferencesChanged_Notification.Builder,
            > { notification ->
            val body = notification.body
            Timber.d("Preferences changed on Steam for account ${body.accountid}")

            // The record in the notification leaves the account ID out; it is beside it.
            records[body.accountid] =
                body.preferences.toBuilder().setAccountid(body.accountid).build()
            publish()
        },
    )

    override fun start(session: CoroutineScope) {
        session.launch {
            val request = CPlayer_GetPerFriendPreferences_Request.newBuilder().build()
            val body = player.getPerFriendPreferences(request).bodyOrNull("GetPerFriendPreferences")
                ?: return@launch

            records.clear()
            body.preferencesList.forEach { records[it.accountid] = it }
            publish()
        }
    }

    /**
     * Sets whether chat messages from [friendId] are notified of on a phone.
     *
     * @return Steam's answer; `Timeout` when there is none. The setting changes only on `OK`.
     */
    suspend fun setMobileNotifications(friendId: Long, setting: MobileNotifications): EResult {
        val accountId = friendId.toSteamID().accountID.toInt()
        val changed = (records[accountId] ?: PerFriendPreferences.getDefaultInstance())
            .toBuilder()
            .setAccountid(accountId)
            .setNotificationsSendmobile(setting.toSteam())
            .build()

        val request = CPlayer_SetPerFriendPreferences_Request.newBuilder().setPreferences(
            changed,
        ).build()
        val result = player.setPerFriendPreferences(request).resultOf("SetPerFriendPreferences")
        if (result == EResult.OK) {
            records[accountId] = changed
            publish()
        }

        return result
    }

    private fun publish() {
        SteamFriendPreferences.set(
            records.values
                .associate {
                    it.accountid.accountIdToSteamID().toLong() to
                        it.notificationsSendmobile.toSetting()
                }
                .filterValues { it != MobileNotifications.Default },
        )
    }

    private fun ENotificationSetting.toSetting(): MobileNotifications = when (this) {
        ENotificationSetting.k_ENotificationSettingAlways -> MobileNotifications.Always
        ENotificationSetting.k_ENotificationSettingNever -> MobileNotifications.Never
        else -> MobileNotifications.Default
    }

    private fun MobileNotifications.toSteam(): ENotificationSetting = when (this) {
        MobileNotifications.Default -> ENotificationSetting.k_ENotificationSettingNotifyUseDefault
        MobileNotifications.Always -> ENotificationSetting.k_ENotificationSettingAlways
        MobileNotifications.Never -> ENotificationSetting.k_ENotificationSettingNever
    }
}
