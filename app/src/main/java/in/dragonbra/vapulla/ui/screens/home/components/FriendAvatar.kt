package `in`.dragonbra.vapulla.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.util.Utils

@Composable
fun FriendAvatar(friend: SteamFriend) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                color = friend.statusColor,
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center,
        content = {
            AsyncImage(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small),
                model = Utils.getAvatarURL(friend.avatar),
                contentDescription = "Avatar for ${friend.name}",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.vapulla),
                error = painterResource(R.drawable.vapulla)
            )
        }
    )
}