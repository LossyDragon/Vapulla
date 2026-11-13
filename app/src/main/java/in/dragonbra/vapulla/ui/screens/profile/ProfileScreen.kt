package `in`.dragonbra.vapulla.ui.screens.profile

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
import `in`.dragonbra.vapulla.service.api.FavoriteBadge
import `in`.dragonbra.vapulla.ui.composables.BBCodeText
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.DetailListSkeleton
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.InfoPillSkeleton
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.dialog.DialogType
import `in`.dragonbra.vapulla.ui.composables.dialog.MessageDialog
import `in`.dragonbra.vapulla.ui.composables.dialog.MessageDialogState
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendAvatar
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

    ProfileScreenContent(
        friend = friend ?: SteamFriend(id = 0),
        profile = profile,
        favoriteBadge = favoriteBadge,
        badgeLoading = badgeLoading,
        onBack = onBack,
        onAlias = viewModel::onAlias,
        onBlock = viewModel::onBlock,
        onRemove = viewModel::onRemove,
        onNickname = viewModel::onNickname,
        onChat = onChat,
    )
}

@Composable
private fun ProfileScreenContent(
    friend: SteamFriend,
    profile: ProfileInfo?,
    favoriteBadge: FavoriteBadge?,
    badgeLoading: Boolean,
    onBack: () -> Unit,
    onAlias: () -> Unit,
    onBlock: () -> Unit,
    onRemove: () -> Unit,
    onChat: (Long) -> Unit,
    onNickname: (String) -> Unit,
) {
    val context = LocalContext.current
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

            DialogType.FRIEND_FAVORITE ->
                Toast.makeText(context, "Favorite TODO", Toast.LENGTH_SHORT).show()

            DialogType.FRIEND_UN_FAVORITE ->
                Toast.makeText(context, "Un-Favorite TODO", Toast.LENGTH_SHORT).show()

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

    val actions = listOf(
        ProfileAction(Icons.Outlined.History, "View aliases") {
            onAlias()
            showAliasesDialog = true
        },
        ProfileAction(Icons.Outlined.Edit, "Set nickname") { showNicknameDialog = true },
        ProfileAction(Icons.Outlined.FavoriteBorder, "Add to favorites") {
            msgDialogState = MessageDialogState(
                visible = true,
                type = DialogType.FRIEND_FAVORITE,
                confirmBtnText = "Favorite",
                title = "Favorite Friend",
                message = "Add ${friend.nameOrNickname} to favorites?",
            )
        },
        ProfileAction(Icons.Outlined.Notifications, "Set alerts") {
            Toast.makeText(context, "Notifications TODO", Toast.LENGTH_SHORT).show()
        },
        ProfileAction(Icons.Outlined.PersonOff, "Block", destructive = true) {
            msgDialogState = MessageDialogState(
                visible = true,
                type = DialogType.FRIEND_BLOCK,
                confirmBtnText = "Block",
                title = "Block Friend",
                message = "Are you sure you want to block ${friend.nameOrNickname}?",
            )
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
            contentPadding = listContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfileHeader(
                    friend = friend,
                    favoriteBadge = favoriteBadge,
                    badgeLoading = badgeLoading,
                )
            }

            item {
                PrimaryActions(
                    onChat = { onChat(friend.id) },
                    onSteamProfile = { uriHandler.openUri(Utils.getProfileUrl(friend.id)) },
                )
            }

            item { Section(title = "About") { About(profile) } }

            item { Section(title = "Manage") { ActionList(actions) } }
        }
    }
}

/** Avatar, name and presence, centred at the top of the profile. */
@Composable
private fun ProfileHeader(
    friend: SteamFriend,
    favoriteBadge: FavoriteBadge?,
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
        FriendAvatar(size = 128.dp, friend = friend)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = friend.nameOrNickname,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineMediumEmphasized,
        )

        // A nickname hides the Steam name, so show that one underneath.
        if (friend.nickname.isNotEmpty() && friend.name.isNotEmpty()) {
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

        // A placeholder holds the pill's spot while the badge loads, and the row folds away
        // if the friend turns out to have none.
        AnimatedVisibility(visible = badgeLoading || favoriteBadge != null) {
            if (favoriteBadge != null) {
                InfoPill(
                    modifier = Modifier.padding(top = 4.dp),
                    text = "Favorite badge · level ${favoriteBadge.level}" +
                        if (favoriteBadge.appId > 0) " · app ${favoriteBadge.appId}" else "",
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
private fun PrimaryActions(onChat: () -> Unit, onSteamProfile: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onChat,
            modifier = Modifier.weight(1f),
            shapes = ButtonDefaults.shapes(),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
            Text(text = "  Chat")
        }
        FilledTonalButton(
            onClick = onSteamProfile,
            modifier = Modifier.weight(1f),
            shapes = ButtonDefaults.shapes(),
        ) {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
            Text(text = "  Steam profile")
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
            favoriteBadge = FavoriteBadge(badgeId = 1, appId = 440, level = 5),
            badgeLoading = false,
            onBack = {},
            onAlias = {},
            onBlock = {},
            onRemove = {},
            onChat = {},
            onNickname = {},
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
            badgeLoading = true,
            onBack = {},
            onAlias = {},
            onBlock = {},
            onRemove = {},
            onChat = {},
            onNickname = {},
        )
    }
}
