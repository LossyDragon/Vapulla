package `in`.dragonbra.vapulla.ui.screens.home.components

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import `in`.dragonbra.vapulla.data.entity.SteamFriend

@Composable
fun FriendName(friend: SteamFriend) {
    Text(
        text = buildAnnotatedString {
            append(friend.nameOrNickname)
            if (friend.nickname.isNotEmpty()) {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                    ),
                ) {
                    append(" * ")
                }
            } else {
                append(" ")
            }
            appendInlineContent("icon", "[icon]")
        },
        inlineContent = mapOf(
            "icon" to InlineTextContent(
                Placeholder(
                    width = 14.sp,
                    height = 14.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
                children = {
                    friend.statusIcon?.let {
                        Icon(
                            imageVector = it,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = it.name,
                        )
                    }
                },
            ),
        ),
    )
}