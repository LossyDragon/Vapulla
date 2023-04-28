package `in`.dragonbra.vapulla.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.dragonbra.vapulla.service.SteamService

class LogOutReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val logoffRequest = Intent(context, SteamService::class.java).apply {
            putExtra(SteamService.EXTRA_ACTION, "stop")
        }

        context.startService(logoffRequest)
    }
}
