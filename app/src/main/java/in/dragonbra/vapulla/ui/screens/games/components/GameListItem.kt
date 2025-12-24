package `in`.dragonbra.vapulla.ui.screens.games.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import `in`.dragonbra.vapulla.util.Utils.toDateString

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameListItem(
    modifier: Modifier = Modifier,
    app: SteamApp,
    localAccountId: Long? = null,
    onGameClicked: () -> Unit,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onGameClicked),
        overlineContent = { Text(text = app.type.name.uppercase()) },
        headlineContent = { Text(text = app.name.decodeHtml()) },
        supportingContent = {
            Column {
                Text(text = "Developer: ${app.developer.decodeHtml()}")
                Text(text = "Publisher: ${app.publisher.decodeHtml()}")
                if (app.releaseDate != 0L) {
                    Text(text = "Released: ${app.releaseDate.toDateString()}")
                }
                if (app.metacriticScore > 0) {
                    val score = buildAnnotatedString {
                        append("Metacritic Score: ")
                        withStyle(
                            style = SpanStyle(
                                color = when (app.metacriticScore) {
                                    in 0..49 -> Color.Red
                                    in 50..74 -> Color(0xFFFF9800)
                                    in 75..100 -> Color.Green
                                    else -> Color.Gray
                                },
                            ),
                        ) {
                            append(app.metacriticScore.toString())
                        }
                    }
                    Text(text = score)
                }
            }
        },
        leadingContent = {
            CoilImage(
                imageModel = { app.clientIconUrl },
                modifier = Modifier.size(56.dp),
                loading = {
                    Box(
                        contentAlignment = Alignment.Center,
                        content = { LoadingIndicator() },
                    )
                },
                previewPlaceholder = painterResource(R.drawable.vapulla_background),
                failure = {
                    Box(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "Failed to load image",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        trailingContent = if (!app.ownerAccountId.contains(localAccountId?.toInt())) {
            {
                Icon(imageVector = Icons.Outlined.Groups, contentDescription = null)
            }
        } else {
            null
        },

    )
}

@Preview
@Composable
private fun Preview() {
    VapullaTheme {
        GameListItem(
            app = SteamApp(
                id = 440,
                name = "Team Fortress 2",
                type = SteamApp.AppType.game,
                releaseDate = 1191999600,
                metacriticScore = 92,
                developer = "Valve",
                publisher = "Valve",
            ),
            onGameClicked = {},
        )
    }
}
