@file:Suppress("BooleanMethodIsAlwaysInverted")

package `in`.dragonbra.vapulla.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.UpdateMachineAuthCallback
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import org.spongycastle.util.encoders.Hex

class AccountManager(private val context: Context) {

    companion object {
        private const val KEY_AVATAR_HASH = "account_avatar_hash"
        private const val KEY_LOGIN_KEY = "account_login_key"
        private const val KEY_NICKNAME = "account_nickname"
        private const val KEY_STATE = "account_state"
        private const val KEY_STEAM_ID = "account_steam_id"
        private const val KEY_UNIQUE_ID = "account_unique_id"
        private const val KEY_USERNAME = "account_username"

        private const val SENTRY_FILE_NAME = "sentry.bin"
    }

    var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        private set

    private val listeners = mutableSetOf<AccountManagerListener>()

    val hasSentryFile: Boolean
        get() = File(context.filesDir, SENTRY_FILE_NAME).exists()

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

    val sentrySize: Long
        get() = File(context.filesDir, SENTRY_FILE_NAME).length()

    fun updateSentryFile(callback: UpdateMachineAuthCallback) {
        val sentryFile = File(context.filesDir, SENTRY_FILE_NAME)
        FileOutputStream(sentryFile).use { fos ->
            val byteBuffer = ByteBuffer.wrap(callback.data, 0, callback.bytesToWrite)
            fos.channel.run {
                position(callback.offset.toLong())
                write(byteBuffer)
            }
        }
    }

    fun clear() {
        prefs.edit {
            remove(KEY_LOGIN_KEY)
            remove(KEY_UNIQUE_ID)
            remove(KEY_USERNAME)
            remove(KEY_STEAM_ID)
            remove(KEY_AVATAR_HASH)
            remove(KEY_NICKNAME)
            remove(KEY_STATE)
        }
    }

    fun readSentryFile(): ByteArray {
        val file = File(context.filesDir, SENTRY_FILE_NAME)

        val digest = MessageDigest.getInstance("SHA-1")

        val buffer = ByteArray(8192)
        var n = 0

        FileInputStream(file).use {
            while (n != -1) {
                n = it.read(buffer)

                if (n > 0) {
                    digest.update(buffer, 0, n)
                }
            }
        }

        return digest.digest()
    }

    fun saveLocalUser(personaState: PersonaState) {
        avatarHash = Hex.toHexString(personaState.avatarHash)
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
