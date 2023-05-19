package `in`.dragonbra.vapulla.broadcastreceiver

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.dragonbra.vapulla.service.SteamService
import timber.log.Timber

class ReplyReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ID = "id"
        const val RESULT_KEY = "RESULT_KEY"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive $intent")
        RemoteInput.getResultsFromIntent(intent)?.let { remoteInput ->
            val id = intent.getLongExtra(EXTRA_ID, 0L)

            Timber.d("onReceive id $id")

            if (id == 0L) {
                return
            }

            val input = remoteInput.getCharSequence(RESULT_KEY)

            val replyReceiver = Intent(context, SteamService::class.java).apply {
                putExtra(SteamService.EXTRA_ACTION, "reply")
                putExtra(SteamService.EXTRA_ID, id)
                putExtra(SteamService.EXTRA_MESSAGE, input)
            }

            context.startService(replyReceiver)
        }
    }
}
