package `in`.dragonbra.vapulla.compose.screens.invites.links

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.shareLink
import `in`.dragonbra.vapulla.core.getCreatedTime
import `in`.dragonbra.vapulla.core.getInviteURL
import `in`.dragonbra.vapulla.model.InviteTokenItem
import kotlin.random.Random

@Composable
fun InviteLinkCard(
    token: InviteTokenItem,
    steamID: SteamID,
    universe: EUniverse,
    onDelete: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val tokenLink = remember {
        getInviteURL(steamID, universe, token.inviteToken)
    }

    val tokenCreated = remember {
        getCreatedTime(token.timeCreated)
    }

    ListItem(
        headlineContent = {
            Text(text = tokenLink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(text = tokenCreated)
        },
        trailingContent = {
            Row {
                // TODO delete
//                IconButton(onClick = { onDelete(token.inviteToken) }) {
//                    Icon(Icons.Default.DeleteForever, "Share Link")
//                }
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(tokenLink))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                ) {
                    Icon(Icons.Default.ContentCopy, "Copy Link")
                }
                IconButton(onClick = { context.shareLink(tokenLink) }) {
                    Icon(Icons.Default.Share, "Share Link")
                }
            }
        }
    )
}

@Preview
@Composable
private fun Preview_InviteLinkCard() {
    fun timeCreated(): Long = Random.nextLong(1_000_000_000L, 9_999_999_999L + 1)

    fun inviteDuration(): Long = 30L * 24 * 60 * 60 * 1000

    fun inviteToken(): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..8)
            .map { Random.nextInt(0, characters.length) }
            .map(characters::get)
            .joinToString("")
    }

    VapullaTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            InviteLinkCard(
                token = InviteTokenItem(
                    inviteToken = inviteToken(),
                    inviteLimit = 1,
                    inviteDuration = inviteDuration(),
                    timeCreated = timeCreated(),
                    isValid = true
                ),
                steamID = SteamID(76561198003805806),
                universe = EUniverse.Public,
                onDelete = {},
            )
        }
    }
}
