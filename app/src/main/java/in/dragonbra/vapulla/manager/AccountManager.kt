@file:Suppress("BooleanMethodIsAlwaysInverted")

package `in`.dragonbra.vapulla.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStatesCallback

class AccountManager(context: Context) {

    companion object {
        private const val KEY_AVATAR_HASH = "account_avatar_hash"
        private const val KEY_LAST_LOGIN_SUCCESSFUL = "last_login_successful"
        private const val KEY_LOGIN_KEY = "account_login_key"
        private const val KEY_NICKNAME = "account_nickname"
        private const val KEY_STATE = "account_state"
        private const val KEY_STEAM_ID = "account_steam_id"
        private const val KEY_UNIQUE_ID = "account_unique_id"
        private const val KEY_USERNAME = "account_username"
    }

    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val listeners = mutableSetOf<AccountManagerListener>()

    var lastLoginSuccessful: Boolean
        get() = prefs.getBoolean(KEY_LAST_LOGIN_SUCCESSFUL, false)
        set(value) = prefs.edit { putBoolean(KEY_LAST_LOGIN_SUCCESSFUL, value) }

    /* Pref Item */
    var prefClearNotifications: Boolean
        get() = prefs.getBoolean("pref_clear_notifications", false)
        set(value) = prefs.edit { putBoolean("pref_clear_notifications", value) }

    /* Pref Item */
    var prefFriendsListSort: Boolean
        get() = prefs.getBoolean("pref_friends_list_sort", true)
        set(value) = prefs.edit { putBoolean("pref_friends_list_sort", value) }

    /* Pref Item */
    var prefFriendsListRecents: Long
        get() = prefs.getLong("pref_friends_list_recents", 604800000)
        set(value) = prefs.edit { putLong("pref_friends_list_recents", value) }

    var loginKey: String?
        get() = prefs.getString(KEY_LOGIN_KEY, null)
        set(value) = prefs.edit { putString(KEY_LOGIN_KEY, value) }

    var uniqueId: Int
        get() = prefs.getInt(KEY_UNIQUE_ID, 0)
        set(value) = prefs.edit { putInt(KEY_UNIQUE_ID, value) }

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }

    var nickname: String?
        get() = prefs.getString(KEY_NICKNAME, null)
        set(value) = prefs.edit { putString(KEY_NICKNAME, value) }

    var steamId: Long
        get() = prefs.getLong(KEY_STEAM_ID, 0L)
        set(value) = prefs.edit { putLong(KEY_STEAM_ID, value) }

    var avatarHash: String?
        get() = prefs.getString(KEY_AVATAR_HASH, null)
        set(value) = prefs.edit { putString(KEY_AVATAR_HASH, value) }

    var state: EPersonaState
        get() = EPersonaState.from(prefs.getInt(KEY_STATE, 0))
        set(value) = prefs.edit { putInt(KEY_STATE, value.code()) }

    fun getCollapsedState(pref: String): Boolean {
        return prefs.getBoolean("is_${pref}_collapsed", false)
    }

    fun setCollapsedState(pref: String, value: Boolean) {
        prefs.edit { putBoolean("is_${pref}_collapsed", value) }
    }

    fun clear() {
        prefs.edit {
            remove(KEY_AVATAR_HASH)
            remove(KEY_LAST_LOGIN_SUCCESSFUL)
            remove(KEY_LOGIN_KEY)
            remove(KEY_NICKNAME)
            remove(KEY_STATE)
            remove(KEY_STEAM_ID)
            remove(KEY_UNIQUE_ID)
            remove(KEY_USERNAME)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun saveLocalUser(personaState: PersonaStatesCallback) {
        avatarHash = personaState.avatarHash.toHexString()
        nickname = personaState.name
        steamId = personaState.friendID.convertToUInt64()
        state = personaState.state

        listeners.forEach {
            it.onAccountUpdate(this@AccountManager)
        }
    }

    fun addListener(l: AccountManagerListener) = listeners.add(l)

    fun removeListener(l: AccountManagerListener) = listeners.remove(l)

    interface AccountManagerListener {
        fun onAccountUpdate(account: AccountManager)
    }
}
