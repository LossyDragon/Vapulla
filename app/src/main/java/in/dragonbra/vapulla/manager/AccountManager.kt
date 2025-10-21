package `in`.dragonbra.vapulla.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class AccountManager(private val context: Context) {

    private companion object {
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferences")

        val ACCOUNT_NAME = stringPreferencesKey("account_name")
        val ACCOUNT_AVATAR_HASH = stringPreferencesKey("account_avatar_hash")
        val ACCOUNT_PERSONA_STATE = intPreferencesKey("account_state")
        val ACCOUNT_STEAMID = longPreferencesKey("account_steamid")

        val LOGIN_USERNAME = stringPreferencesKey("account_username")
        val LOGIN_REFRESH_TOKEN = stringPreferencesKey("account_refresh_token")
        val LOGIN_UUID = intPreferencesKey("account_uuid")

        val LAST_CHANGE_NUMBER = intPreferencesKey("last_change_number")
    }

    val username = context.dataStore.data
        .catch { handleException(it) }
        .map { it[LOGIN_USERNAME] }

    val refreshToken = context.dataStore.data
        .catch { handleException(it) }
        .map { it[LOGIN_REFRESH_TOKEN] }

    val uuid = context.dataStore.data
        .catch { handleException(it) }
        .map { it[LOGIN_UUID] }

    val steamid = context.dataStore.data
        .catch { handleException(it) }
        .map { it[ACCOUNT_STEAMID] }

    val lastChangeNumber = context.dataStore.data
        .catch { handleException(it) }
        .map { it[LAST_CHANGE_NUMBER] }

    val accountName = context.dataStore.data
        .catch { handleException(it) }
        .map { it[ACCOUNT_NAME] }

    val accountAvatar = context.dataStore.data
        .catch { handleException(it) }
        .map { it[ACCOUNT_AVATAR_HASH] }

    val accountPersonaState = context.dataStore.data
        .catch { handleException(it) }
        .map { it[ACCOUNT_PERSONA_STATE] }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[LOGIN_USERNAME] = name
        }
    }

    suspend fun setRefreshToken(token: String?) {
        context.dataStore.edit { prefs ->
            if (token != null) {
                prefs[LOGIN_REFRESH_TOKEN] = token
            } else {
                prefs.remove(LOGIN_REFRESH_TOKEN)
            }
        }
    }

    suspend fun setUuid(uuid: Int) {
        context.dataStore.edit { prefs ->
            prefs[LOGIN_UUID] = uuid
        }
    }

    suspend fun setLastChangeNumber(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[LAST_CHANGE_NUMBER] = value
        }
    }

    suspend fun saveLocalUser(localUser: PersonaStateCallback) {
        context.dataStore.edit { prefs ->
            prefs[ACCOUNT_NAME] = localUser.playerName
            prefs[ACCOUNT_AVATAR_HASH] = localUser.avatarHash.toHexString()
            prefs[ACCOUNT_PERSONA_STATE] = localUser.personaState.code()
            prefs[ACCOUNT_STEAMID] = localUser.friendId.convertToUInt64()
        }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit {
            it.clear()
        }
    }

    private fun handleException(exception: Throwable): Preferences {
        if (exception is IOException) {
            return emptyPreferences()
        } else {
            throw exception
        }
    }
}