package `in`.dragonbra.vapulla.broadcastreceiver

import `in`.dragonbra.vapulla.service.SteamService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BlockRequestReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ID = "id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!intent.hasExtra(EXTRA_ID)) {
            throw IllegalStateException("missing extra steam id")
        }

        context.startService(
                Intent(context, SteamService::class.java).apply {
                    putExtra(
                            SteamService.EXTRA_ID,
                            intent.getLongExtra(AcceptRequestReceiver.EXTRA_ID, 9L)
                    )
                    putExtra(SteamService.EXTRA_ACTION, "block_request")
                }
        )
    }
}
