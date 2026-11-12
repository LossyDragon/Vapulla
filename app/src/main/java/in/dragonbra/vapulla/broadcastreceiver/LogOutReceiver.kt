package `in`.dragonbra.vapulla.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.dragonbra.vapulla.service.SteamService

/** Asks [SteamService] to log off and stop, e.g. from the foreground notification. */
class LogOutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.startService(SteamService.actionIntent(context, SteamService.ACTION_STOP))
    }
}
