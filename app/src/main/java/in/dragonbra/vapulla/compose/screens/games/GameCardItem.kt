package `in`.dragonbra.vapulla.compose.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.StaticImage
import `in`.dragonbra.vapulla.compose.util.formatPlayTime
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.retrofit.response.Game

@Composable
fun GameCardItem(game: Game, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(88.dp)) {
            StaticImage(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp)),
                url = String.format(Constants.GAME_LOGO_URL, game.appid),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                failure = {
                    val color = MaterialTheme.colorScheme.surface.copy(alpha = .5f)
                    Column(
                        modifier = Modifier
                            .background(color)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = game.name, textAlign = TextAlign.Center)
                    }
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        id = R.string.textPlayedRecent,
                        formatPlayTime(game.playtimeTwoWeeks ?: 0)
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(
                        id = R.string.textPlayedForever,
                        formatPlayTime(game.playtimeForever)
                    ),
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview
@Composable
private fun GameCardItem_Preview() {
    VapullaTheme {
        LazyColumn(Modifier.fillMaxSize()) {
            items(12) {
                GameCardItem(
                    game = Game(
                        appid = 440,
                        name = "Team Fortress 2",
                        playtimeForever = 669,
                        playtimeTwoWeeks = 696,
                        imgIconUrl = null
                    ),
                    onItemClick = {}
                )
            }
        }
    }
}
