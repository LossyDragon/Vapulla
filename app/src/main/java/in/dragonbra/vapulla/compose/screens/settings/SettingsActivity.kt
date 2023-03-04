package `in`.dragonbra.vapulla.compose.screens.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.screens.login.LoginActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.threading.executeAsyncTask
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : VapullaBaseActivity() {

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var db: VapullaDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("onCreate")

        setContent {
            val scope = rememberCoroutineScope()
            VapullaTheme {
                CompositionLocalProvider(LocalActivity provides this) {
                    SettingsScreen(
                        accountManager = accountManager,
                        onChangeName = { name ->
                            if (name.isEmpty()) {
                                return@SettingsScreen
                            }

                            scope.executeAsyncTask {
                                steamService?.getHandler<SteamFriends>()?.setPersonaName(name).let {
                                    accountManager.nickname = name
                                }
                            }
                        },
                        onChangeUser = {
                            scope.executeAsyncTask(
                                doInBackground = { steamService?.disconnect() },
                                onPostExecute = { clearData() }
                            )
                        },
                        onClearDatabase = { clearDatabase() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        onServiceStart()
    }

    override fun onDisconnected() {
        super.onDisconnected()
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    private fun clearData() {
        scope.executeAsyncTask {
            accountManager.clear()
            clearDatabase()
            accountManager.prefs.edit().clear().apply()
        }
    }

    private fun clearDatabase() {
        scope.executeAsyncTask {
            db.steamFriendDao().delete()
            db.chatMessageDao().delete()
            db.emoticonDao().delete()
            db.gameSchemaDao().delete()
        }
    }
}
