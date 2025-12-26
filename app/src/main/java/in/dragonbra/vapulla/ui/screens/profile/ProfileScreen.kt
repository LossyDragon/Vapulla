package `in`.dragonbra.vapulla.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.ktx.isLight
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.ProfileInfoCallback
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.data.ProfileInfo
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.composables.BBCodeText
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.dialog.DialogType
import `in`.dragonbra.vapulla.ui.composables.dialog.MessageDialog
import `in`.dragonbra.vapulla.ui.composables.dialog.MessageDialogState
import `in`.dragonbra.vapulla.ui.screens.profile.components.ProfileButton
import `in`.dragonbra.vapulla.util.Utils
import java.util.Date

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onNavDrawerAction: () -> Unit, onBack: () -> Unit) {
    val friend by viewModel.friend.collectAsStateWithLifecycle()
    val profile by viewModel.friendProfile.collectAsStateWithLifecycle()

    ProfileScreenContent(
        friend = friend ?: SteamFriend(id = 0),
        profile = profile,
        onBack = onBack,
        onAlias = viewModel::onAlias,
        onBlock = viewModel::onBlock,
        onRemove = viewModel::onRemove,
        onNickName = viewModel::onNickName,
        onChat = { },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreenContent(
    friend: SteamFriend,
    profile: ProfileInfo?,
    onBack: () -> Unit,
    onAlias: () -> Unit,
    onBlock: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onChat: (Long) -> Unit,
    onNickName: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var msgDialogState by rememberSaveable(stateSaver = MessageDialogState.Saver) {
        mutableStateOf(MessageDialogState(false))
    }

    val onDismissRequest: (() -> Unit)?
    val onDismissClick: (() -> Unit)?
    val onConfirmClick: (() -> Unit)?

    when (msgDialogState.type) {
        DialogType.FRIEND_BLOCK -> {
            onConfirmClick = {
                onBlock(friend.id)
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissRequest = { msgDialogState = MessageDialogState(visible = false) }
            onDismissClick = { msgDialogState = MessageDialogState(visible = false) }
        }

        DialogType.FRIEND_REMOVE -> {
            onConfirmClick = {
                onRemove(friend.id)
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissRequest = { msgDialogState = MessageDialogState(visible = false) }
            onDismissClick = { msgDialogState = MessageDialogState(visible = false) }
        }

        DialogType.FRIEND_FAVORITE -> {
            onConfirmClick = {
                Toast.makeText(context, "Favorite TODO", Toast.LENGTH_SHORT).show()
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissRequest = { msgDialogState = MessageDialogState(visible = false) }
            onDismissClick = { msgDialogState = MessageDialogState(visible = false) }
        }

        DialogType.FRIEND_UN_FAVORITE -> {
            onConfirmClick = {
                Toast.makeText(context, "Un-Favorite TODO", Toast.LENGTH_SHORT).show()
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissRequest = { msgDialogState = MessageDialogState(visible = false) }
            onDismissClick = { msgDialogState = MessageDialogState(visible = false) }
        }

        else -> {
            onDismissRequest = null
            onDismissClick = null
            onConfirmClick = null
        }
    }

    MessageDialog(
        visible = msgDialogState.visible,
        onDismissRequest = onDismissRequest,
        onConfirmClick = onConfirmClick,
        confirmBtnText = msgDialogState.confirmBtnText,
        onDismissClick = onDismissClick,
        dismissBtnText = msgDialogState.dismissBtnText,
        icon = msgDialogState.type.icon,
        title = msgDialogState.title,
        message = msgDialogState.message,
    )

    var setNickNameDialog by rememberSaveable { mutableStateOf(false) }
    var newNickName by rememberSaveable(friend.nickname) {
        mutableStateOf(friend.nickname)
    }
    if (setNickNameDialog) {
        AlertDialog(
            onDismissRequest = {
                setNickNameDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                )
            },
            title = { Text(text = "Set Nickname") },
            text = {
                Column {
                    Text(
                        text = "Set a new nickname for " + friend.name + "?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newNickName,
                        onValueChange = { newNickName = it },
                        label = { Text(text = "Nickname") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        setNickNameDialog = false
                        onNickName(newNickName)
                    },
                    content = { Text(text = "OK") },
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { setNickNameDialog = false },
                    content = { Text(text = "Cancel") },
                )
            },
        )
    }

    var showPreviousAliasDialog by rememberSaveable { mutableStateOf(false) }
    if (showPreviousAliasDialog) {
        AlertDialog(
            onDismissRequest = {
                showPreviousAliasDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                )
            },
            title = { Text(text = "Past Aliases") },
            text = {
                LazyColumn {
                    items(friend.aliases) { alias ->
                        Text(text = alias)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (friend.aliases.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "No past aliases found")
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPreviousAliasDialog = false },
                    content = { Text(text = "Close") },
                )
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    BackButton(onClick = onBack)
                },
            )
        },
    ) { paddingValues ->
        val uriHandler = LocalUriHandler.current
        val isLight = MaterialTheme.colorScheme.background.isLight()
        var moreExpanded by rememberSaveable { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            CoilImage(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .size(92.dp),
                imageModel = { Utils.getAvatarURL(friend.avatar) },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                ),
                loading = { CircularProgressIndicator() },
                failure = { Icon(Icons.Filled.QuestionMark, null) },
                previewPlaceholder = painterResource(R.drawable.vapulla_background),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = friend.nameOrNickname,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineLarge,
            )

            Text(
                text = friend.isPlayingGameName,
                color = if (isLight) Color.Unspecified else friend.statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileButton(
                    icon = Icons.AutoMirrored.Outlined.Chat,
                    text = "Chat",
                    onClick = { onChat(friend.id) },
                )
                Spacer(modifier = Modifier.width(16.dp))
                ProfileButton(
                    icon = Icons.Outlined.Person,
                    text = "Profile",
                    onClick = { uriHandler.openUri(Utils.getProfileUrl(friend.id)) },
                )
                Spacer(modifier = Modifier.width(16.dp))
                ProfileButton(
                    icon = Icons.Outlined.Games,
                    text = "Games",
                    onClick = { /* TODO */ },
                )
                Spacer(modifier = Modifier.width(16.dp))
                ProfileButton(
                    icon = Icons.Outlined.MoreVert,
                    text = if (!moreExpanded) "More" else "Less",
                    onClick = { moreExpanded = !moreExpanded },
                )
            }

            AnimatedVisibility(visible = moreExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileButton(
                            icon = Icons.Outlined.History,
                            text = "View Aliases",
                            onClick = {
                                onAlias()
                                showPreviousAliasDialog = true
                            },
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ProfileButton(
                            icon = Icons.Outlined.Edit,
                            text = "Set Nickname",
                            onClick = {
                                setNickNameDialog = true
                            },
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ProfileButton(
                            icon = Icons.Outlined.PersonOff,
                            text = "Block Friend",
                            onClick = {
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.FRIEND_BLOCK,
                                    confirmBtnText = "Block",
                                    dismissBtnText = "Cancel",
                                    title = "Block Friend",
                                    message =
                                        "Are you sure you want to block " + friend.nameOrNickname +
                                            "?",
                                )
                            },
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ProfileButton(
                            icon = Icons.Outlined.PersonRemove,
                            text = "Remove Friend",
                            onClick = {
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.FRIEND_REMOVE,
                                    confirmBtnText = "Remove",
                                    dismissBtnText = "Cancel",
                                    title = "Remove Friend",
                                    message =
                                        "Are you sure you want to remove " + friend.nameOrNickname +
                                            "?",
                                )
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileButton(
                            icon = Icons.Outlined.Favorite,
                            text = "Add to Favorites",
                            onClick = {
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.FRIEND_FAVORITE,
                                    confirmBtnText = "Favorite",
                                    dismissBtnText = "Cancel",
                                    title = "Favorite Friend",
                                    message = "Add $Unit to Favorites", // TODO
                                )
                            },
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ProfileButton(
                            icon = Icons.Outlined.Notifications,
                            text = "Set Alerts",
                            onClick = {
                                Toast.makeText(context, "Notifications TODO", Toast.LENGTH_SHORT)
                                    .show()
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    if (profile == null) {
                        LoadingBox()
                    } else {
                        // 'headline' doesn't seem to be used anymore
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                        ) {
                            // Meh...
                            with(profile) {
                                // Steam launch: Sept 12, 2003
                                val isValid = timeCreated.after(Date(1063267200000L))
                                if (isValid) {
                                    if (realName.isNotEmpty()) {
                                        Text(text = "Name: $realName")
                                    }
                                    if (cityName.isNotEmpty()) {
                                        Text(text = "City: $cityName")
                                    }
                                    if (stateName.isNotEmpty()) {
                                        Text(text = "State: $stateName")
                                    }
                                    if (stateName.isNotEmpty()) {
                                        Text(text = "Country: $countryName")
                                    }
                                    Text(text = "Created: $timeCreated")
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = "Summary:")
                                    BBCodeText(text = summary)
                                } else {
                                    Text(
                                        text = "Profile most likely private.\nUnable to retrieve info",
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom scroll padding
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
