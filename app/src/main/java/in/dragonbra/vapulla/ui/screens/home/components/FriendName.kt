package `in`.dragonbra.vapulla.ui.screens.home.components

import android.content.res.Configuration
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

@Composable
fun FriendName(friend: SteamFriend) {
    val variantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val annotatedText = remember(friend.name, friend.nickname, friend.statusIcon, variantColor) {
        buildAnnotatedString {
            append(friend.name)
            if (friend.nickname.isNotEmpty()) {
                withStyle(
                    style = SpanStyle(
                        color = variantColor,
                        fontSize = 12.sp,
                    ),
                ) {
                    append(" (${friend.nickname}) ")
                }
            } else {
                append(" ")
            }

            if (friend.statusIcon != null) {
                appendInlineContent("icon", "[icon]")
            }
        }
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val inlineContent = remember(friend.statusIcon, onSurfaceColor) {
        mapOf(
            "icon" to InlineTextContent(
                Placeholder(
                    width = 14.sp,
                    height = 14.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
                children = {
                    friend.statusIcon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            tint = onSurfaceColor,
                            contentDescription = icon.name,
                        )
                    }
                },
            ),
        )
    }

    Text(
        text = annotatedText,
        inlineContent = inlineContent,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            FriendName(
                friend = SteamFriend(
                    id = 0,
                    name = "Actual Name",
                    nickname = "Nick Name",
                    state = EPersonaState.Away
                )
            )
        }
    }
}