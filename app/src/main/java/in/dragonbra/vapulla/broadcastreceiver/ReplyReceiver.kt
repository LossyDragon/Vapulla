package `in`.dragonbra.vapulla.broadcastreceiver

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.dragonbra.vapulla.service.SteamService

/** Forwards an inline notification reply to [SteamService]. */
class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val friendId = intent.getLongExtra(EXTRA_ID, 0L)
        if (friendId == 0L) return

        val message = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT_REPLY)
        if (message.isNullOrBlank()) return

        context.startService(
            SteamService.actionIntent(
                context = context,
                action = SteamService.ACTION_REPLY,
                friendId = friendId,
                message = message,
            ),
        )
    }

    companion object {
        /** Long extra carrying the friend's 64-bit Steam ID. */
        const val EXTRA_ID = "id"

        /** [RemoteInput] result key holding the typed reply. */
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
}
