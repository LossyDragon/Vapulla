package `in`.dragonbra.vapulla.compose.screens.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.screens.login.LoginActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : VapullaBaseActivity() {

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var db: VapullaDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VapullaTheme {
                CompositionLocalProvider(LocalActivity provides this) {
                    SettingsScreen(
                        accountManager = accountManager,
                        onChangeName = ::changeName,
                        onChangeUser = ::changeUser,
                        onClearDatabase = ::clearDatabase
                    )
                }
            }
        }
    }

    override fun onDisconnected() {
        super.onDisconnected()
        Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.also(::startActivity)
    }

    private fun changeName(name: String) {
        if (name.isEmpty()) {
            return
        }

        scope.launch(Dispatchers.IO) {
            getHandler<SteamFriends>()?.setPersonaName(name).let {
                accountManager.nickname = name
            }
        }
    }

    private fun changeUser() {
        scope.launch(Dispatchers.IO) {
            steamService?.disconnect()
            clearData()
        }
    }

    private fun clearData() {
        scope.launch(Dispatchers.IO) {
            accountManager.clear()
            clearDatabase()
        }
    }

    private fun clearDatabase() {
        steamService?.disconnect()

        scope.launch(Dispatchers.IO) {
            db.steamFriendDao().deleteAll()
            db.chatMessageDao().deleteAllMessages()
            db.emoticonDao().deleteAll()
            db.gameSchemaDao().delete()
        }
    }
}
