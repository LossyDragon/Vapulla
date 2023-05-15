package `in`.dragonbra.vapulla.compose.screens.invites

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.model.InviteTokenItem
import `in`.dragonbra.vapulla.service.SteamService
import kotlinx.coroutines.launch
import timber.log.Timber

class InvitesActivity : VapullaBaseActivity() {

    private lateinit var receiver: BroadcastReceiver

    private val viewModel: InvitesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("onCreate")

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val data = if (Constants.isAtLeastT) {
                    intent?.getParcelableArrayListExtra(
                        "invites_list",
                        InviteTokenItem::class.java
                    ).orEmpty()
                } else {
                    @Suppress("DEPRECATION")
                    val data: ArrayList<InviteTokenItem>? =
                        intent?.getParcelableExtra("invites_list")
                    data.orEmpty()
                }

                viewModel.onInvitesList(data)
            }
        }

        setContent {
            VapullaTheme {
                InvitesScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    onGenerateLink = {
                        scope.launch {
                            steamService?.createFriendInviteToken()
                        }
                    },
                    onDeleteInvite = {
                        scope.launch {
                            steamService?.revokeFriendInviteToken(it)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = IntentFilter(SteamService.BROADCAST_INVITES_LIST)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intent)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)

        val steamID = SteamID(steamService!!.accountManager.steamId)
        val steamUniverse = steamID.accountUniverse

        viewModel.setLoggedInInfo(steamID, steamUniverse)
        scope.launch {
            steamService?.getFriendInviteTokens()
        }
    }
}
