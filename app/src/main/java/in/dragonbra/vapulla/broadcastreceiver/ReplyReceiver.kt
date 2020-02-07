package `in`.dragonbra.vapulla.broadcastreceiver

import `in`.dragonbra.vapulla.service.SteamService
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReplyReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ID = "id"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)

        val id = intent.getLongExtra(EXTRA_ID, 0L)

        if (id == 0L) {
            return
        }

        val message = remoteInput.getCharSequence(KEY_TEXT_REPLY)

        context.startService(
                Intent(context, SteamService::class.java).apply {
                    putExtra(SteamService.EXTRA_ACTION, "reply")
                    putExtra(SteamService.EXTRA_ID, id)
                    putExtra(SteamService.EXTRA_MESSAGE, message)
                }
        )
    }
}
