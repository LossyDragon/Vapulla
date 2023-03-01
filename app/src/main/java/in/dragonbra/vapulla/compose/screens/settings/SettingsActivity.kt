package `in`.dragonbra.vapulla.compose.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.rememberCoroutineScope
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.compose.screens.login.LoginActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.threading.executeAsyncTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.Closeable
import java.util.LinkedList
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var db: VapullaDatabase

    private lateinit var steamService: SteamService

    private val subs: MutableList<Closeable?> = LinkedList()

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as SteamService.SteamBinder
            steamService = binder.getService()
            subs.add(steamService.subscribe<DisconnectedCallback> { onDisconnected() })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("onCreate")
        setContent {
            val scope = rememberCoroutineScope()
            VapullaTheme {
                SettingsScreen(
                    accountManager = accountManager,
                    onBackPressed = { finish() },
                    onChangeName = { name ->
                        if (name.isEmpty()) {
                            return@SettingsScreen
                        }

                        scope.executeAsyncTask {
                            steamService.getHandler<SteamFriends>().setPersonaName(name)
                            accountManager.nickname = name
                        }
                    },
                    onChangeUser = {
                        scope.executeAsyncTask(
                            doInBackground = { steamService.disconnect() },
                            onPostExecute = { clearData() }
                        )
                    },
                    onBrowseUrl = { browse(it) }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, SteamService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        subs.forEach { it?.close() }
    }

    private fun onDisconnected() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
    }

    private fun clearData() {
        CoroutineScope(Dispatchers.IO).launch {
            accountManager.clear()
            db.steamFriendDao().delete()
            db.chatMessageDao().delete()
            db.emoticonDao().delete()
            accountManager.prefs.edit().clear().apply()
        }
    }

    private fun browse(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        startActivity(intent)
    }
}
