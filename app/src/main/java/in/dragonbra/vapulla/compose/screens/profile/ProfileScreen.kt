package `in`.dragonbra.vapulla.compose.screens.profile

import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.waterfallPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.MinContrastOfPrimaryVsSurface
import `in`.dragonbra.vapulla.compose.components.VapullaEditDialog
import `in`.dragonbra.vapulla.compose.components.VapullaListDialog
import `in`.dragonbra.vapulla.compose.components.VapullaMessageDialog
import `in`.dragonbra.vapulla.compose.components.contrastAgainst
import `in`.dragonbra.vapulla.compose.components.rememberDominantColorState
import `in`.dragonbra.vapulla.compose.components.verticalGradientScrim
import `in`.dragonbra.vapulla.compose.screens.chat.ChatActivity
import `in`.dragonbra.vapulla.compose.screens.games.GamesActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.getStatusColor
import `in`.dragonbra.vapulla.compose.ui.theme.iconCornerShape
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.compose.util.StaticImage
import `in`.dragonbra.vapulla.compose.util.getAvatarUrl
import `in`.dragonbra.vapulla.compose.util.getFriendName
import `in`.dragonbra.vapulla.compose.util.getStatusIcon
import `in`.dragonbra.vapulla.compose.util.getStatusText
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.model.FriendListItem

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val uriHandler = LocalUriHandler.current
    val activity = LocalActivity.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    /* Set Nickname Dialog */
    var showNicknameDialog by remember { mutableStateOf(false) }
    VapullaEditDialog(
        icon = Icons.Default.Edit,
        title = stringResource(id = R.string.dialogTitleNickname, state.friend?.name ?: ""),
        editTextLabel = stringResource(id = R.string.textLabelNickname),
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
        icon = Icons.Default.History,
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
        positiveText = stringResource(id = R.string.remove),
        negativeText = stringResource(id = R.string.cancel),
        openDialog = showRemoveDialog,
        onPositive = {
            viewModel.removeFriend()
            showRemoveDialog = false
        },
        onNegative = {
            showRemoveDialog = false
        }
    )

    /* Show Block Dialog */
    var showBlockDialog by remember { mutableStateOf(false) }
    VapullaMessageDialog(
        title = stringResource(id = R.string.dialogTitleBlockFriend, state.friend?.name ?: ""),
        message = stringResource(id = R.string.dialogMessageBlockFriend, state.friend?.name ?: ""),
        positiveText = stringResource(id = R.string.block),
        negativeText = stringResource(id = R.string.cancel),
        openDialog = showBlockDialog,
        onPositive = {
            viewModel.blockFriend()
            showBlockDialog = false
        },
        onNegative = {
            showBlockDialog = false
        }
    )

    val onChatClick = remember<() -> Unit> {
        {
            Intent(context, ChatActivity::class.java).apply {
                putExtra(ProfileActivity.INTENT_STEAM_ID, state.steamID!!.convertToUInt64())
            }.also { context.startActivity(it) }
        }
    }

    val onGamesClicked = remember<() -> Unit> {
        {
            Intent(context, GamesActivity::class.java).apply {
                val bundle = Bundle().apply {
                    putParcelableArrayList(GamesActivity.INTENT_GAMES, state.gamesList)
                    putString("name", state.friend!!.friendName)
                }
                putExtras(bundle)
            }.also { context.startActivity(it) }
        }
    }

    val onAccountClick = remember {
        {
            val url = Constants.PROFILE_URL + state.steamID!!.convertToUInt64()
            uriHandler.openUri(url)
        }
    }

    val onAlias = remember {
        {
            viewModel.getAlias()
            showAliasDialog = true
        }
    }

    ProfileScreenContent(
        state = state,
        onBackPressed = { activity.finish() },
        onChatClick = onChatClick,
        onAccountClick = onAccountClick,
        onGamesClick = onGamesClicked,
        onNickName = { showNicknameDialog = true },
        onAliases = onAlias,
        onRemove = { showRemoveDialog = true },
        onBlock = { showBlockDialog = true }
    )
}

@Composable
private fun ProfileScreenContent(
    state: ProfileState,
    onBackPressed: () -> Unit,
    onChatClick: () -> Unit,
    onAccountClick: () -> Unit,
    onGamesClick: () -> Unit,
    onNickName: () -> Unit,
    onAliases: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit
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
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .waterfallPadding(),
            containerColor = Color.Transparent,
            topBar = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            tint = dominantColorState.onColor,
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileScreenProfileIcon(state = state)

                ProfileScreenNameAndStatus(state = state)

                ProfileScreenInfo(state = state)

                ProfileScreenButtons(
                    color = dominantColorState.onColor,
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
                        onBlock = onBlock
                    )
                }
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
        val borderStroke = BorderStroke(4.dp, getStatusColor(state.friend))
        val avatarUrl = remember(state.friend?.avatar) { getAvatarUrl(state.friend?.avatar) }
        StaticImage(
            modifier = Modifier
                .size(150.dp)
                .border(borderStroke, iconCornerShape)
                .clip(iconCornerShape),
            url = avatarUrl
        )
    }
}

@Composable
private fun ProfileScreenNameAndStatus(state: ProfileState) {
    val context = LocalContext.current
    val friendName = remember(state.friend) { getFriendName(friend = state.friend) }
    val status = remember(state.friend) { context.getStatusText(state.friend) }
    val statusColor = remember(state.friend) { getStatusColor(state.friend) }
    val statusIcon = remember(state.friend) { getStatusIcon(state.friend) }

    Text(
        text = friendName,
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
            text = status,
            color = statusColor,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        statusIcon?.let {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = it,
                tint = statusColor,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun ProfileScreenInfo(state: ProfileState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileLevelLayout(
                modifier = Modifier.weight(1f),
                title = stringResource(id = R.string.textProfileLevel),
                levelNumber = (state.levelCount ?: 0).toString(),
                isLoading = state.isLoading
            )

            Divider(
                modifier = Modifier
                    .width(2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .height(64.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            ProfileLevelLayout(
                modifier = Modifier.weight(1f),
                title = stringResource(id = R.string.textProfileGames),
                levelNumber = (state.gamesCount ?: 0).toString(),
                isLoading = state.isLoading
            )
        }
    }
}

@Composable
private fun ProfileLevelLayout(
    modifier: Modifier,
    title: String,
    levelNumber: String,
    isLoading: Boolean
) {
    Column(
        modifier = modifier.height(80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 5.dp),
                strokeWidth = 2.dp
            )
        }

        AnimatedVisibility(
            visible = !isLoading,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Text(
                text = levelNumber,
                modifier = Modifier.padding(top = 5.dp),
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun ProfileScreenButtons(
    color: Color,
    onChatClick: () -> Unit,
    onAccountClick: () -> Unit,
    onGamesClick: () -> Unit,
    onManageClick: () -> Unit
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
            colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = .7f)),
            onClick = onChatClick
        ) {
            Text(
                text = stringResource(id = R.string.buttonSendMessage),
                color = Color.White
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = .5f)),
            onClick = onAccountClick
        ) {
            Text(
                text = stringResource(id = R.string.buttonViewAccount),
                color = Color.White
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = .5f)),
            onClick = onGamesClick
        ) {
            Text(
                text = stringResource(id = R.string.buttonViewGames),
                color = Color.White
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = .5f)),
            onClick = onManageClick
        ) {
            Text(
                text = stringResource(id = R.string.buttonManageFriend),
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
    onBlock: () -> Unit
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
                onClick = onNickName
            ) {
                Text(
                    text = stringResource(id = R.string.buttonAddNickname),
                    color = Color.White
                )
            }

            OutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                onClick = onAliases
            ) {
                Text(
                    text = stringResource(id = R.string.buttonViewAliases),
                    color = Color.White
                )
            }
        }

        Row {
            OutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                onClick = onRemove
            ) {
                Text(
                    text = stringResource(id = R.string.buttonRemoveFriend),
                    color = Color.White
                )
            }

            OutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                onClick = onBlock
            ) {
                Text(
                    text = stringResource(id = R.string.block),
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
        gameName = "Sleeping Simulator",
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
            onBackPressed = {},
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
