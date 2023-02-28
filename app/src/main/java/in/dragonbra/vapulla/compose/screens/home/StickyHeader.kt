package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.waterfallPadding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme

@Composable
fun StickyHeaderItem(header: String, count: Int) {
    Column {
        Text(
            text = "$header ($count)",
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .padding(start = 16.dp, top = 6.dp, bottom = 6.dp)
                .waterfallPadding()
        )
        Divider(Modifier.fillMaxWidth())
    }
}

@Preview
@Composable
private fun Preview_StickyHeaderItem() {
    val friend = FriendListItem(
        id = 0,
        state = EPersonaState.Online.code(),
        avatar = null,
        gameAppId = 440,
        gameName = "Team Fortess 2",
        lastLogOff = 0L,
        lastLogOn = 0L,
        lastMessage = null,
        lastMessageTime = null,
        name = "Name The Game",
        newMessageCount = null,
        nickname = null,
        relation = 0,
        stateFlags = 0,
        typingTs = 0L
    )
    VapullaTheme {
        Surface {
            Column {
                StickyHeaderItem("Online", 60)
                FriendItem(
                    friend = friend,
                    onClickChat = {},
                    onClickProfile = {},
                    onClickAccept = {},
                    onClickIgnore = {},
                    onClickBlock = {}
                )
            }
        }
    }
}
