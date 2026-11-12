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
import `in`.dragonbra.vapulla.util.TokenCipher
import `in`.dragonbra.vapulla.util.helpers.toLong
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Persists the signed-in account: the login name and refresh token used to sign in again,
 * the local user's persona as last reported by Steam, and the PICS change number.
 */
class AccountManager(private val context: Context) {

    private companion object {
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferences")

        val ACCOUNT_NAME = stringPreferencesKey("account_name")
        val ACCOUNT_AVATAR_HASH = stringPreferencesKey("account_avatar_hash")
        val ACCOUNT_PERSONA_STATE = intPreferencesKey("account_state")
        val ACCOUNT_STEAMID = longPreferencesKey("account_steamid")
        val ACCOUNT_LEVEL = intPreferencesKey("account_level")

        val LOGIN_USERNAME = stringPreferencesKey("account_username")
        val LOGIN_REFRESH_TOKEN = stringPreferencesKey("account_refresh_token")
        val LOGIN_UUID = intPreferencesKey("account_uuid")

        val LAST_CHANGE_NUMBER = intPreferencesKey("last_change_number")
    }

    val username: Flow<String?> = preference(LOGIN_USERNAME)

    /**
     * The refresh token Steam issued at sign-in, which is all that is needed to sign in again;
     * the password is never stored. It is kept encrypted, see [TokenCipher], and decrypted
     * here. Null when there is none or it can no longer be decrypted.
     */
    val refreshToken: Flow<String?> = preference(LOGIN_REFRESH_TOKEN)
        // Decrypting goes through the Keystore; skip it when another preference changed.
        .distinctUntilChanged()
        .map { stored -> stored?.let(TokenCipher::decrypt) }
        .flowOn(Dispatchers.IO)
    val uuid: Flow<Int?> = preference(LOGIN_UUID)
    val steamid: Flow<Long?> = preference(ACCOUNT_STEAMID)
    val lastChangeNumber: Flow<Int?> = preference(LAST_CHANGE_NUMBER)
    val accountName: Flow<String?> = preference(ACCOUNT_NAME)
    val accountAvatar: Flow<String?> = preference(ACCOUNT_AVATAR_HASH)
    val accountPersonaState: Flow<Int?> = preference(ACCOUNT_PERSONA_STATE)
    val accountLevel: Flow<Int?> = preference(ACCOUNT_LEVEL)

    /** Steam level from `Player.GetGameBadgeLevels`, refreshed on each login. */
    suspend fun setLevel(level: Int) {
        context.dataStore.edit { it[ACCOUNT_LEVEL] = level }
    }

    suspend fun setUsername(name: String) {
        context.dataStore.edit { it[LOGIN_USERNAME] = name }
    }

    /** Saves the refresh token, encrypted; null forgets it. */
    suspend fun setRefreshToken(token: String?) {
        val sealed = token?.let { withContext(Dispatchers.IO) { TokenCipher.encrypt(it) } }

        context.dataStore.edit { prefs ->
            if (sealed != null) {
                prefs[LOGIN_REFRESH_TOKEN] = sealed
            } else {
                prefs.remove(LOGIN_REFRESH_TOKEN)
            }
        }
    }

    /**
     * Encrypts a refresh token that an older build saved as plain text. Called after each
     * successful sign-in; does nothing once the stored token is in the encrypted form.
     */
    suspend fun encryptStoredRefreshToken() {
        val stored = preference(LOGIN_REFRESH_TOKEN).first() ?: return
        if (!TokenCipher.isEncrypted(stored)) {
            setRefreshToken(stored)
        }
    }

    suspend fun setUuid(uuid: Int) {
        context.dataStore.edit { it[LOGIN_UUID] = uuid }
    }

    suspend fun setLastChangeNumber(value: Int) {
        context.dataStore.edit { it[LAST_CHANGE_NUMBER] = value }
    }

    /** Stores the local user's persona from the [PersonaStateCallback] Steam sends about us. */
    suspend fun saveLocalUser(localUser: PersonaStateCallback) {
        context.dataStore.edit { prefs ->
            prefs[ACCOUNT_NAME] = localUser.playerName
            prefs[ACCOUNT_AVATAR_HASH] = localUser.avatarHash.toHexString()
            prefs[ACCOUNT_PERSONA_STATE] = localUser.personaState.code()
            prefs[ACCOUNT_STEAMID] = localUser.friendId.toLong()
        }
    }

    /** Forgets the saved sign-in so the next launch asks for credentials again. */
    suspend fun clearLogin() {
        context.dataStore.edit { prefs ->
            prefs.remove(LOGIN_USERNAME)
            prefs.remove(LOGIN_REFRESH_TOKEN)
        }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit { it.clear() }
    }

    /**
     * One preference as a flow. A failed read of the backing file yields empty preferences
     * instead of ending the flow; any other error still propagates.
     */
    private fun <T> preference(key: Preferences.Key<T>): Flow<T?> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it[key] }
}
