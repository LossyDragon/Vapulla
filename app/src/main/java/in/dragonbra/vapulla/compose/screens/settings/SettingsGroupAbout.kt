package `in`.dragonbra.vapulla.compose.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.R

@Composable
fun SettingsGroupAbout() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    SettingsGroup(title = { Text(text = stringResource(id = R.string.textSettingsAbout)) }) {
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.textSettingsVersion)) },
            subtitle = { Text(text = BuildConfig.VERSION_NAME) },
            onClick = { /* Unit */ }
        )
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.textSettingsRateApp)) },
            enabled = false,
            onClick = {
                val pkgName = context.packageName
                val url = "https://play.google.com/store/apps/details?id=$pkgName"
                uriHandler.openUri(url)
            }
        )
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.textSettingsSourceCode)) },
            onClick = {
                val url = "https://github.com/Longi94/Vapulla"
                uriHandler.openUri(url)
            }
        )
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.textSettingsLicenses)) },
            onClick = {
                val githubUrl = "https://raw.githubusercontent.com"
                val url = "$githubUrl/Longi94/Vapulla/master/third_party.txt"
                uriHandler.openUri(url)
            }
        )

        Divider(
            Modifier
                .padding(top = 2.dp)
                .fillMaxWidth()
        )
    }
}
