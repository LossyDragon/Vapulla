package `in`.dragonbra.vapulla.compose.screens.invites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.util.shareLink
import `in`.dragonbra.vapulla.model.InviteTokenItem

@Composable
fun InviteGenerateCard(
    onGenerateLink: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
        ) {
            Text(
                color = Color.White,
                fontSize = 24.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                text = stringResource(id = R.string.quick_invite_generate_title)
            )
            Spacer(modifier = Modifier.height(height = 12.dp))
            Text(
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                text = stringResource(id = R.string.quick_invite_generate_message)
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = onGenerateLink) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = Color.White,
                        lineHeight = 16.sp,
                        style = MaterialTheme.typography.labelLarge,
                        text = stringResource(id = R.string.generate),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun InviteLinkCard(
    modifier: Modifier,
    state: InvitesState,
    token: InviteTokenItem,
    onDeleteInvite: (token: String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val tokenLink by remember(token.inviteToken) {
        val url = InviteUtils.getInviteURL(
            state.loggedInUniverse,
            state.loggedInSteamID!!,
            token.inviteToken
        )

        mutableStateOf(url)
    }

    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp),
                value = tokenLink,
                supportingText = {
                    val created = InviteUtils.getCreatedTime(token.timeCreated)
                    Text(
                        text = stringResource(
                            id = R.string.quick_invite_created,
                            created
                        )
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { onDeleteInvite(token.inviteToken) }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(tokenLink))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                ) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 5.dp
                        ),
                        color = Color.White,
                        lineHeight = 16.sp,
                        style = MaterialTheme.typography.labelLarge,
                        text = stringResource(id = R.string.copy),
                        textAlign = TextAlign.Center
                    )
                }
                Button(onClick = { context.shareLink(tokenLink) }) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 5.dp
                        ),
                        color = Color.White,
                        lineHeight = 16.sp,
                        style = MaterialTheme.typography.labelLarge,
                        text = stringResource(id = R.string.share),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
