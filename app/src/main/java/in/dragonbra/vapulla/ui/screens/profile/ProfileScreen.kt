package `in`.dragonbra.vapulla.ui.screens.profile

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.ktx.isLight
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.ProfileInfo
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.LocalAppSettings
import `in`.dragonbra.vapulla.service.api.EquippedProfileItems
import `in`.dragonbra.vapulla.service.api.FavoriteBadge
import `in`.dragonbra.vapulla.service.api.MobileNotifications
import `in`.dragonbra.vapulla.ui.composables.BBCodeText
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.DetailListSkeleton
import `in`.dragonbra.vapulla.ui.composables.IconLabel
import `in`.dragonbra.vapulla.ui.composables.ImageOptions
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.InfoPillSkeleton
import `in`.dragonbra.vapulla.ui.composables.LoopingVideo
import `in`.dragonbra.vapulla.ui.composables.RemoteImage
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.dialog.DialogType
import `in`.dragonbra.vapulla.ui.composables.dialog.MessageDialog
import `in`.dragonbra.vapulla.ui.composables.dialog.MessageDialogState
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendAvatar
import `in`.dragonbra.vapulla.ui.screens.home.components.friendDisplayName
import `in`.dragonbra.vapulla.ui.screens.home.components.statusColor
import `in`.dragonbra.vapulla.ui.screens.home.components.statusText
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.Utils.toDateString
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit, onChat: (Long) -> Unit) {
    val friend by viewModel.friend.collectAsStateWithLifecycle()
    val profile by viewModel.friendProfile.collectAsStateWithLifecycle()
    val favoriteBadge by viewModel.favoriteBadge.collectAsStateWithLifecycle()
    val badgeLoading by viewModel.badgeLoading.collectAsStateWithLifecycle()
    val mutualFriends by viewModel.mutualFriends.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val equipped by viewModel.equipped.collectAsStateWithLifecycle()
    val mobileNotifications by viewModel.mobileNotifications.collectAsStateWithLifecycle()

    ProfileScreenContent(
        friend = friend ?: SteamFriend(id = 0),
        profile = profile,
        favoriteBadge = favoriteBadge,
        equipped = equipped,
        badgeLoading = badgeLoading,
        mutualFriends = mutualFriends,
        onBack = onBack,
        onAlias = viewModel::loadAliases,
        onBlock = viewModel::toggleBlocked,
        onRemove = viewModel::removeFriend,
        onNickname = viewModel::setNickname,
        isFavorite = isFavorite,
        onFavorite = viewModel::toggleFavorite,
        mobileNotifications = mobileNotifications,
        onMobileNotifications = viewModel::setMobileNotifications,
        onChat = onChat,
    )
}

@Composable
private fun ProfileScreenContent(
    friend: SteamFriend,
    profile: ProfileInfo?,
    favoriteBadge: FavoriteBadge?,
    equipped: EquippedProfileItems,
    badgeLoading: Boolean,
    mutualFriends: ImmutableList<String>?,
    onBack: () -> Unit,
    onAlias: () -> Unit,
    onBlock: () -> Unit,
    onRemove: () -> Unit,
    onChat: (Long) -> Unit,
    onNickname: (String) -> Unit,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    mobileNotifications: MobileNotifications,
    onMobileNotifications: (MobileNotifications) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    var msgDialogState by rememberSaveable(stateSaver = MessageDialogState.Saver) {
        mutableStateOf(MessageDialogState(false))
    }
    val dismissDialog = { msgDialogState = MessageDialogState(visible = false) }

    // One confirmation dialog serves every action that needs one; the type says which.
    val confirmDialog = {
        when (msgDialogState.type) {
            DialogType.FRIEND_BLOCK -> onBlock()
            DialogType.FRIEND_REMOVE -> onRemove()
            DialogType.NONE -> Unit
        }
        dismissDialog()
    }

    MessageDialog(
        visible = msgDialogState.visible,
        onDismissRequest = dismissDialog,
        onConfirmClick = confirmDialog,
        confirmBtnText = msgDialogState.confirmBtnText,
        onDismissClick = dismissDialog,
        dismissBtnText = msgDialogState.dismissBtnText,
        icon = msgDialogState.type.icon,
        title = msgDialogState.title,
        message = msgDialogState.message,
    )

    var showNotificationsDialog by rememberSaveable { mutableStateOf(false) }
    if (showNotificationsDialog) {
        MobileNotificationsDialog(
            selected = mobileNotifications,
            onApply = {
                showNotificationsDialog = false
                onMobileNotifications(it)
            },
            onDismiss = { showNotificationsDialog = false },
        )
    }

    var showNicknameDialog by rememberSaveable { mutableStateOf(false) }
    if (showNicknameDialog) {
        NicknameDialog(
            friend = friend,
            onDismiss = { showNicknameDialog = false },
            onConfirm = { nickname ->
                showNicknameDialog = false
                onNickname(nickname)
            },
        )
    }

    var showAliasesDialog by rememberSaveable { mutableStateOf(false) }
    if (showAliasesDialog) {
        AliasesDialog(aliases = friend.aliases, onDismiss = { showAliasesDialog = false })
    }

    val actions = listOfNotNull(
        ProfileAction(Icons.Outlined.History, "View aliases") {
            onAlias()
            showAliasesDialog = true
        },
        ProfileAction(Icons.Outlined.Edit, "Set nickname") { showNicknameDialog = true },
        if (friend.isFriend) {
            ProfileAction(
                icon = Icons.Outlined.Notifications,
                label = "Mobile notifications: ${mobileNotifications.label}",
                onClick = { showNotificationsDialog = true },
            )
        } else {
            null
        },
        // Steam keeps favourites for friends; someone blocked or only asking is not one.
        if (friend.isFriend) {
            ProfileAction(
                icon = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                label = if (isFavorite) "Remove from favorites" else "Add to favorites",
                onClick = onFavorite,
            )
        } else {
            null
        },
        if (friend.isBlocked) {
            ProfileAction(Icons.Outlined.Person, "Unblock") {
                msgDialogState = MessageDialogState(
                    visible = true,
                    type = DialogType.FRIEND_BLOCK,
                    confirmBtnText = "Unblock",
                    title = "Unblock Friend",
                    message = "Unblock ${friend.nameOrNickname}?",
                )
            }
        } else {
            ProfileAction(Icons.Outlined.PersonOff, "Block", destructive = true) {
                msgDialogState = MessageDialogState(
                    visible = true,
                    type = DialogType.FRIEND_BLOCK,
                    confirmBtnText = "Block",
                    title = "Block Friend",
                    message = "Are you sure you want to block ${friend.nameOrNickname}?",
                )
            }
        },
        ProfileAction(Icons.Outlined.PersonRemove, "Remove friend", destructive = true) {
            msgDialogState = MessageDialogState(
                visible = true,
                type = DialogType.FRIEND_REMOVE,
                confirmBtnText = "Remove",
                title = "Remove Friend",
                message = "Are you sure you want to remove ${friend.nameOrNickname}?",
            )
        },
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Profile") },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
            // Room below the last row, so the folded "Under the hood" header can be scrolled
            // clear of the bottom edge instead of sitting right on it.
            contentPadding = listContentPadding(padding, extraBottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfileHeader(
                    friend = friend,
                    favoriteBadge = favoriteBadge,
                    equipped = equipped,
                    badgeLoading = badgeLoading,
                )
            }

            item {
                PrimaryActions(
                    canChat = !friend.isBlocked,
                    onChat = { onChat(friend.id) },
                    onSteamProfile = { uriHandler.openUri(Utils.getProfileUrl(friend.id)) },
                )
            }

            // Only for someone who sent a friend request: who they already know.
            if (mutualFriends != null) {
                item { Section(title = "Mutual friends") { MutualFriends(mutualFriends) } }
            }

            item { Section(title = "About") { About(profile) } }

            item { Section(title = "Manage") { ActionList(actions) } }

            item { FriendDebugSection(friend) }
        }
    }
}

/** The friends the sender of a friend request has in common with the account. */
@Composable
private fun MutualFriends(names: ImmutableList<String>) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (names.isEmpty()) {
                "None of your friends is friends with them."
            } else {
                names.joinToString()
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

private val AvatarSize = 128.dp

/** Steam draws an avatar frame this much larger than the avatar it goes around. */
private const val FRAME_SCALE = 1.22f

/**
 * The avatar on the background the user picked for their mini profile, or failing that for
 * their profile. A background that moves is played as a silent loop, which fades in when it
 * starts. Without a background the avatar stands alone.
 */
@Composable
private fun ProfileBanner(friend: SteamFriend, equipped: EquippedProfileItems) {
    val background = equipped.miniBackground ?: equipped.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge),
        contentAlignment = Alignment.Center,
    ) {
        if (background != null) {
            // Steam's still of a moving background is framed differently from the video, so
            // going from one to the other shows as a jump. The still is therefore only for a
            // background that does not move, or whose video will not play.
            var videoFailed by remember(background) { mutableStateOf(false) }
            val showVideo = background.movieUrl.isNotEmpty() && !videoFailed

            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            )
            if (showVideo) {
                LoopingVideo(
                    url = background.movieUrl,
                    modifier = Modifier.matchParentSize(),
                    onError = { videoFailed = true },
                )
            } else {
                RemoteImage(
                    imageModel = { background.imageLargeUrl },
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        DecoratedAvatar(
            friend = friend,
            equipped = equipped,
            modifier = Modifier.padding(vertical = if (background != null) 20.dp else 0.dp),
        )
    }
}

/** The avatar, animated when the user has one that is, with their avatar frame around it. */
@Composable
private fun DecoratedAvatar(
    friend: SteamFriend,
    equipped: EquippedProfileItems,
    modifier: Modifier = Modifier,
) {
    val animatedUrl = equipped.animatedAvatar?.imageSmallUrl.orEmpty()
    val frameUrl = equipped.avatarFrame?.let { it.imageSmallUrl.ifEmpty { it.imageLargeUrl } }

    Box(
        // Room for the frame only when there is one, so a plain avatar sits where it did.
        modifier = modifier.size(if (frameUrl != null) AvatarSize * FRAME_SCALE else AvatarSize),
        contentAlignment = Alignment.Center,
    ) {
        if (animatedUrl.isNotEmpty()) {
            RemoteImage(
                imageModel = { animatedUrl },
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(MaterialTheme.shapes.large),
                imageOptions = ImageOptions(contentDescription = "Avatar for ${friend.name}"),
            )
        } else {
            FriendAvatar(size = AvatarSize, friend = friend)
        }

        if (frameUrl != null) {
            RemoteImage(
                imageModel = { frameUrl },
                modifier = Modifier.matchParentSize(),
                imageOptions = ImageOptions(contentScale = ContentScale.Fit),
            )
        }
    }
}

/** Avatar, name and presence, centred at the top of the profile. */
@Composable
private fun ProfileHeader(
    friend: SteamFriend,
    favoriteBadge: FavoriteBadge?,
    equipped: EquippedProfileItems,
    badgeLoading: Boolean,
) {
    val isLight = MaterialTheme.colorScheme.background.isLight()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ProfileBanner(friend = friend, equipped = equipped)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = friendDisplayName(friend),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineMediumEmphasized,
        )

        // With nicknames shown on their own, the Steam name goes underneath.
        val appendNicknames = LocalAppSettings.current.appendNicknames
        if (!appendNicknames && friend.nickname.isNotEmpty() && friend.name.isNotEmpty()) {
            Text(
                text = friend.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = friend.statusText,
            color = if (isLight) Color.Unspecified else friend.statusColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )

        // The badge the friend shows off, which is not their Steam level: that is not
        // something Steam tells a client about anyone but itself. A placeholder holds the
        // pill's spot while the badge loads, and the row folds away if the friend turns out
        // to have none.
        AnimatedVisibility(visible = badgeLoading || favoriteBadge != null) {
            if (favoriteBadge != null) {
                // Named when the badge is known; one that is not is told by its number.
                val name = favoriteBadge.name.ifEmpty {
                    if (favoriteBadge.appId > 0) {
                        "Badge of app ${favoriteBadge.appId}"
                    } else {
                        "Badge ${favoriteBadge.badgeId}"
                    }
                }

                InfoPill(
                    modifier = Modifier.padding(top = 4.dp),
                    text = "$name · level ${favoriteBadge.level}",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            } else {
                InfoPillSkeleton(modifier = Modifier.padding(top = 4.dp), width = 160.dp)
            }
        }
    }
}

@Composable
private fun PrimaryActions(canChat: Boolean, onChat: () -> Unit, onSteamProfile: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onChat,
            enabled = canChat,
            modifier = Modifier.weight(1f),
            shapes = ButtonDefaults.shapes(),
        ) {
            IconLabel(Icons.AutoMirrored.Outlined.Chat, "Chat")
        }
        FilledTonalButton(
            onClick = onSteamProfile,
            modifier = Modifier.weight(1f),
            shapes = ButtonDefaults.shapes(),
        ) {
            IconLabel(Icons.AutoMirrored.Outlined.OpenInNew, "Steam profile")
        }
    }
}

/** Steam's launch date. A profile "created" before it means Steam returned no real data. */
private val STEAM_LAUNCH = Date(1063267200000L)

/** The public profile details and summary, once Steam has answered. */
@Composable
private fun About(profile: ProfileInfo?) {
    when {
        profile == null -> DetailListSkeleton()

        !profile.timeCreated.after(STEAM_LAUNCH) -> AboutCard {
            Text(
                text = "This profile is private",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        else -> {
            val location = listOf(profile.cityName, profile.stateName, profile.countryName)
                .filter { it.isNotEmpty() }
                .joinToString()

            // Steam ignores the 'headline' field nowadays, so it is not shown.
            DetailList(
                listOf(
                    "Real name" to profile.realName,
                    "Location" to location,
                    "Member since" to (profile.timeCreated.time / 1000).toDateString(),
                ),
            )

            if (profile.summary.isNotBlank()) {
                AboutCard {
                    BBCodeText(
                        text = profile.summary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

/** One row of the "Manage" list. [destructive] actions are drawn in the error colour. */
private class ProfileAction(
    val icon: ImageVector,
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun ActionList(actions: List<ProfileAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        actions.forEachIndexed { index, action ->
            Surface(
                onClick = action.onClick,
                shape = groupedListShape(index, actions.size),
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = if (action.destructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = action.icon, contentDescription = null)
                    Text(text = action.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun NicknameDialog(
    friend: SteamFriend,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var nickname by rememberSaveable { mutableStateOf(friend.nickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Outlined.Edit, contentDescription = null) },
        title = { Text(text = "Set Nickname") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Set a new nickname for ${friend.name}?",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(text = "Nickname") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nickname) }) { Text(text = "OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        },
    )
}

/** How each choice is worded, on the profile and in the dialog. */
private val MobileNotifications.label: String
    get() = when (this) {
        MobileNotifications.Default -> "Default"
        MobileNotifications.Always -> "Always"
        MobileNotifications.Never -> "Never"
    }

/**
 * Picks whether this friend's chat messages are notified of on a phone. The setting is
 * Steam's, shared with its own app, and this app's notifications follow it. Nothing is
 * sent to Steam until Apply is pressed.
 */
@Composable
private fun MobileNotificationsDialog(
    selected: MobileNotifications,
    onApply: (MobileNotifications) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by rememberSaveable { mutableStateOf(selected) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null) },
        title = { Text(text = "Mobile notifications") },
        text = {
            Column {
                Text(
                    text = "For chat messages from this friend. \"Default\" follows your " +
                        "Steam notification settings.",
                )
                MobileNotifications.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .selectable(
                                selected = option == picked,
                                role = Role.RadioButton,
                                onClick = { picked = option },
                            )
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The row is the click target; the button only shows the state.
                        RadioButton(selected = option == picked, onClick = null)
                        Text(text = option.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = picked != selected, onClick = { onApply(picked) }) {
                Text(text = "Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        },
    )
}

@Composable
private fun AliasesDialog(aliases: ImmutableList<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Outlined.History, contentDescription = null) },
        title = { Text(text = "Past Aliases") },
        text = {
            if (aliases.isEmpty()) {
                Text(text = "No past aliases found")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(aliases) { alias -> Text(text = alias) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "Close") }
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        ProfileScreenContent(
            friend = SteamFriend(
                id = 1,
                name = "Gamer",
                nickname = "Player One",
                state = EPersonaState.Online,
                gameAppID = 440,
                gameName = "Team Fortress 2",
                aliases = persistentListOf("Gamer", "OldGamer"),
            ),
            profile = ProfileInfo(
                steamID = SteamID(1L),
                timeCreated = Date(1_191_999_600_000),
                realName = "A. Gamer",
                cityName = "Bellevue",
                stateName = "Washington",
                countryName = "United States",
                headline = "",
                summary = "Plays [b]Team Fortress 2[/b] most evenings.",
            ),
            favoriteBadge = FavoriteBadge(1, appId = 440, level = 5, name = "Team Fortress 2 badge"),
            equipped = EquippedProfileItems(),
            badgeLoading = false,
            mutualFriends = persistentListOf("Ann", "Bob"),
            onBack = {},
            onAlias = {},
            onBlock = {},
            onRemove = {},
            onChat = {},
            onNickname = {},
            isFavorite = false,
            onFavorite = {},
            mobileNotifications = MobileNotifications.Default,
            onMobileNotifications = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_Private() {
    VapullaTheme {
        ProfileScreenContent(
            friend = SteamFriend(id = 1, name = "Gamer"),
            profile = ProfileInfo(
                steamID = SteamID(1L),
                timeCreated = Date(0),
                realName = "",
                cityName = "",
                stateName = "",
                countryName = "",
                headline = "",
                summary = "",
            ),
            favoriteBadge = null,
            equipped = EquippedProfileItems(),
            badgeLoading = true,
            mutualFriends = null,
            onBack = {},
            onAlias = {},
            onBlock = {},
            onRemove = {},
            onChat = {},
            onNickname = {},
            isFavorite = false,
            onFavorite = {},
            mobileNotifications = MobileNotifications.Default,
            onMobileNotifications = {},
        )
    }
}
