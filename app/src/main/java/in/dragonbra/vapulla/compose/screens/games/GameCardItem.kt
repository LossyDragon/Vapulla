package `in`.dragonbra.vapulla.compose.screens.games

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.formatPlayTime
import `in`.dragonbra.vapulla.core.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameCardItem(
    imageLoader: ImageLoader,
    appID: Int,
    title: String,
    recentPlayTime: Int?,
    totalPlayTime: Int,
    onItemClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onItemClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
                CoilImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    imageLoader = { imageLoader },
                    imageRequest = {
                        ImageRequest.Builder(context)
                            .data(String.format(Constants.GAME_LOGO_URL, appID))
                            .crossfade(true)
                            .build()
                    },
                    imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                    previewPlaceholder = R.mipmap.ic_launcher_foreground,
                    loading = {
                        Box(
                            modifier = Modifier.size(128.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    },
                    failure = {
                        Box(
                            modifier = Modifier.size(128.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
            Box(modifier = Modifier.padding(16.dp)) {
                Column {
                    Text(
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        text = title.trimStart()
                    )
                    Text(
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        text = stringResource(
                            id = R.string.textPlayedRecent,
                            formatPlayTime(recentPlayTime ?: 0)
                        )
                    )
                    Text(
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        text = stringResource(
                            id = R.string.textPlayedForever,
                            formatPlayTime(totalPlayTime)
                        )
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
        }
    }
}

@Preview
@Composable
private fun GameCardItem_Preview() {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }.diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(1.0)
                .build()
        }.build()

    VapullaTheme {
        LazyColumn(Modifier.fillMaxSize()) {
            items(12) {
                GameCardItem(
                    imageLoader = imageLoader,
                    appID = 440,
                    title = "Team Fortress 2",
                    recentPlayTime = 60,
                    totalPlayTime = 4140,
                    onItemClick = {}
                )
            }
        }
    }
}
