package `in`.dragonbra.vapulla.compose.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.waterfallPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.components.MinContrastOfPrimaryVsSurface
import `in`.dragonbra.vapulla.compose.components.VapullaEditDialog
import `in`.dragonbra.vapulla.compose.components.VapullaListDialog
import `in`.dragonbra.vapulla.compose.components.VapullaMessageDialog
import `in`.dragonbra.vapulla.compose.components.contrastAgainst
import `in`.dragonbra.vapulla.compose.components.rememberDominantColorState
import `in`.dragonbra.vapulla.compose.components.verticalGradientScrim
import `in`.dragonbra.vapulla.compose.ui.theme.Shapes
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.colorPrimary
import `in`.dragonbra.vapulla.compose.ui.theme.colorSecondary
import `in`.dragonbra.vapulla.compose.ui.theme.getStatusColor
import `in`.dragonbra.vapulla.compose.util.friendNameBuilder
import `in`.dragonbra.vapulla.compose.util.getAvatarUrl
import `in`.dragonbra.vapulla.compose.util.getStatusIcon
import `in`.dragonbra.vapulla.compose.util.getStatusText
import `in`.dragonbra.vapulla.retrofit.response.Games

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onChatClick: (steamID: SteamID) -> Unit,
    onAccountClick: (steamID: SteamID) -> Unit,
    onGamesClick: (gamesList: ArrayList<Games>, name: String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    /* Set Nickname Dialog */
    var showNicknameDialog by remember { mutableStateOf(false) }
    VapullaEditDialog(
        name = state.friend?.name ?: "",
        currentName = state.friend?.nickname,
        openDialog = showNicknameDialog,
        onConfirm = {
            viewModel.setNickname(it)
            showNicknameDialog = false
        },
        onDismiss = {
            showNicknameDialog = false
        }
    )

    /* Show Aliases Dialog */
    var showAliasDialog by remember { mutableStateOf(false) }
    VapullaListDialog(
        title = stringResource(id = R.string.dialogTitleAliases),
        list = state.aliasHistory,
        openDialog = showAliasDialog,
        onDismiss = { showAliasDialog = false }
    )

    /* Show Remove Dialog */
    var showRemoveDialog by remember { mutableStateOf(false) }
    VapullaMessageDialog(
        title = stringResource(id = R.string.dialogTitleRemoveFriend, state.friend?.name ?: ""),
        message = stringResource(id = R.string.dialogMessageRemoveFriend, state.friend?.name ?: ""),
        openDialog = showRemoveDialog,
        onConfirm = {
            viewModel.removeFriend()
            showRemoveDialog = false
        },
        onDismiss = {
            showRemoveDialog = false
        }
    )

    /* Show Block Dialog */
    var showBlockDialog by remember { mutableStateOf(false) }
    VapullaMessageDialog(
        title = stringResource(id = R.string.dialogTitleBlockFriend, state.friend?.name ?: ""),
        message = stringResource(id = R.string.dialogMessageBlockFriend, state.friend?.name ?: ""),
        openDialog = showBlockDialog,
        onConfirm = {
            viewModel.blockFriend()
            showBlockDialog = false
        },
        onDismiss = {
            showBlockDialog = false
        }
    )

    ProfileScreenContent(
        state = state,
        onChatClick = { onChatClick(state.steamID!!) },
        onAccountClick = { onAccountClick(state.steamID!!) },
        onGamesClick = { onGamesClick(state.gamesList, state.friend!!.friendName) },
        onNickName = { showNicknameDialog = true },
        onAliases = {
            viewModel.getAlias()
            showAliasDialog = true
        },
        onRemove = {
            showRemoveDialog = true
        },
        onBlock = { showBlockDialog = true },
    )
}

@Composable
private fun ProfileScreenContent(
    state: ProfileState,
    onChatClick: () -> Unit,
    onAccountClick: () -> Unit,
    onGamesClick: () -> Unit,
    onNickName: () -> Unit,
    onAliases: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit,
) {
    var isManageVisible by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val surfaceColor = MaterialTheme.colorScheme.surface
    val dominantColorState = rememberDominantColorState(
        defaultColor = MaterialTheme.colorScheme.surface,
        isColorValid = {
            it.contrastAgainst(surfaceColor) >= MinContrastOfPrimaryVsSurface
        }
    )

    LaunchedEffect(state.friend?.avatar) {
        dominantColorState.updateColorsFromImageUrl(getAvatarUrl(state.friend?.avatar))
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .verticalGradientScrim(
                color = dominantColorState.color.copy(alpha = 0.50f),
                startYPercentage = 1f,
                endYPercentage = 0f
            ),
        color = Color.Transparent,
        shape = Shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .waterfallPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            ProfileScreenProfileIcon(state = state)

            ProfileScreenNameAndStatus(state = state)

            ProfileScreenInfo(state = state)

            ProfileScreenButtons(
                onChatClick = onChatClick,
                onGamesClick = onGamesClick,
                onAccountClick = onAccountClick,
                onManageClick = { isManageVisible = !isManageVisible }
            )

            AnimatedVisibility(visible = isManageVisible) {
                ProfileExpandedButtons(
                    onNickName = onNickName,
                    onAliases = onAliases,
                    onRemove = onRemove,
                    onBlock = onBlock,
                )
            }
        }
    }
}

@Composable
private fun ProfileScreenProfileIcon(state: ProfileState) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current

        val borderStroke = BorderStroke(4.dp, getStatusColor(state.friend))
        val cornerShape = RoundedCornerShape(16.dp)
        CoilImage(
            modifier = Modifier
                .size(150.dp)
                .border(borderStroke, cornerShape)
                .clip(cornerShape),
            imageRequest = {
                ImageRequest.Builder(context)
                    .data(getAvatarUrl(state.friend?.avatar))
                    .crossfade(true)
                    .build()
            },
            previewPlaceholder = R.drawable.vapulla,
            imageOptions = ImageOptions(requestSize = IntSize(150, 150))
        )
    }
}

@Composable
private fun ProfileScreenNameAndStatus(state: ProfileState) {
    Text(
        text = friendNameBuilder(friend = state.friend),
        modifier = Modifier.padding(5.dp),
        fontSize = 32.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = getStatusText(state.friend),
            color = getStatusColor(state.friend),
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        getStatusIcon(state.friend)?.let {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = it,
                tint = getStatusColor(state.friend),
                contentDescription = null
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ProfileScreenInfo(state: ProfileState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorPrimary)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.textLevel),
                    color = Color.White,
                    fontSize = 20.sp
                )

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 5.dp),
                        color = colorSecondary,
                        strokeWidth = 2.dp,
                    )
                }

                AnimatedVisibility(
                    visible = !state.isLoading,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Text(
                        text = (state.levelCount ?: 0).toString(),
                        modifier = Modifier.padding(top = 5.dp),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }

            Divider(
                modifier = Modifier
                    .width(2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .height(64.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.textGames),
                    color = Color.White,
                    fontSize = 20.sp
                )

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 5.dp),
                        color = colorSecondary,
                        strokeWidth = 2.dp,
                    )
                }

                AnimatedVisibility(
                    visible = !state.isLoading,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Text(
                        text = (state.gamesCount ?: 0).toString(),
                        modifier = Modifier.padding(top = 5.dp),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileScreenButtons(
    onChatClick: () -> Unit,
    onAccountClick: () -> Unit,
    onGamesClick: () -> Unit,
    onManageClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .fillMaxHeight(.5f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colorSecondary),
            onClick = onChatClick,
        ) {
            Text(
                text = stringResource(id = R.string.buttonChat),
                color = Color.White
            )

        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colorSecondary),
            onClick = onAccountClick,
        ) {
            Text(
                text = stringResource(id = R.string.buttonViewAccount),
                color = Color.White
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colorSecondary),
            onClick = onGamesClick,
        ) {
            Text(
                text = stringResource(id = R.string.buttonViewGames),
                color = Color.White
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colorSecondary),
            onClick = onManageClick,
        ) {
            Text(
                text = stringResource(id = R.string.buttonManage),
                color = Color.White
            )
        }
    }
}

@Composable
private fun ProfileExpandedButtons(
    onNickName: () -> Unit,
    onAliases: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .fillMaxHeight(.5f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row {
            OutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                onClick = onNickName,
            ) {
                Text(
                    text = stringResource(id = R.string.menuSetNickname),
                    color = Color.White
                )
            }

            OutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                onClick = onAliases,
            ) {
                Text(
                    text = stringResource(id = R.string.menuViewAliases),
                    color = Color.White
                )
            }
        }

        Row {
            OutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                onClick = onRemove,
            ) {
                Text(
                    text = stringResource(id = R.string.menuRemoveFriend),
                    color = Color.White
                )
            }

            OutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                onClick = onBlock,
            ) {
                Text(
                    text = stringResource(id = R.string.menuBlock),
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_ProfileScreenContent() {
    val friend = FriendListItem(
        avatar = "17683cb013b8f4cd6ef1d1b1aa47036da2413d8e",
        gameAppId = 100,
        gameName = "Chimken Eating Simulator",
        id = 0,
        lastLogOff = 0,
        lastLogOn = 0,
        lastMessage = null,
        lastMessageTime = 0,
        name = "Lu",
        newMessageCount = 50,
        nickname = "Fat Cat",
        relation = EFriendRelationship.Friend.code(),
        state = EPersonaState.Online.code(),
        stateFlags = 512,
        typingTs = 0
    )
    VapullaTheme {
        ProfileScreenContent(
            state = ProfileState(
                friend = friend,
                isLoading = true,
                gamesCount = 888,
                levelCount = 100
            ),
            onChatClick = {},
            onAccountClick = {},
            onGamesClick = {},
            onNickName = {},
            onAliases = {},
            onRemove = {},
            onBlock = {}
        )
    }
}