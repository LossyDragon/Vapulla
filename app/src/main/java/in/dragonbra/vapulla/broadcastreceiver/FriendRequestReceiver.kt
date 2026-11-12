package `in`.dragonbra.vapulla.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.dragonbra.vapulla.service.SteamService

/**
 * Base for the friend-request notification actions. Each subclass forwards the tapped
 * action and the friend's Steam ID to [SteamService], which talks to Steam.
 *
 * @param action one of the `SteamService.ACTION_*` request actions.
 */
abstract class FriendRequestReceiver(private val action: String) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        check(intent.hasExtra(EXTRA_ID)) { "Missing friend Steam ID extra" }

        context.startService(
            SteamService.actionIntent(
                context = context,
                action = action,
                friendId = intent.getLongExtra(EXTRA_ID, 0L),
            ),
        )
    }

    companion object {
        /** Long extra carrying the friend's 64-bit Steam ID. */
        const val EXTRA_ID = "id"
    }
}

/** Accepts a pending friend request. */
class AcceptRequestReceiver : FriendRequestReceiver(SteamService.ACTION_ACCEPT_REQUEST)

/** Declines a pending friend request. */
class IgnoreRequestReceiver : FriendRequestReceiver(SteamService.ACTION_IGNORE_REQUEST)

/** Declines a pending friend request and blocks the sender. */
class BlockRequestReceiver : FriendRequestReceiver(SteamService.ACTION_BLOCK_REQUEST)
