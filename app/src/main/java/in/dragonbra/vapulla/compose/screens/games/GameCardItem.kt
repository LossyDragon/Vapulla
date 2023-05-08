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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        modifier = Modifier
            .clip(shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
                CoilImage(
                    modifier = Modifier.fillMaxWidth(),
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp)
            ) {
                Text(
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.W500,
                    lineHeight = 32.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    text = title.trimStart() // Thanks Dead Space
                )
                Spacer(modifier = Modifier.height(height = 12.dp))
                Text(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontSize = 14.sp),
                    text = stringResource(
                        id = R.string.textPlayedRecent,
                        formatPlayTime(recentPlayTime ?: 0)
                    )
                )
                Text(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontSize = 14.sp),
                    text = stringResource(
                        id = R.string.textPlayedForever,
                        formatPlayTime(totalPlayTime)
                    )
                )
                Spacer(modifier = Modifier.height(height = 16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(onClick = onItemClick) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            color = Color.White,
                            lineHeight = 16.sp,
                            style = MaterialTheme.typography.labelLarge,
                            text = "Visit Store",
                            textAlign = TextAlign.Center
                        )
                    }
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
