package `in`.dragonbra.vapulla.ui.screens.login.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LoginButtons(
    onSignInViaCredentials: () -> Unit,
    onSignInViaQR: () -> Unit,
) {
    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = onSignInViaCredentials,
                    content = {
                        Icon(
                            Icons.AutoMirrored.Filled.Login,
                            modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Credential Login")
                    }
                )
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    onClick = onSignInViaQR,
                    content = {
                        Icon(
                            Icons.Filled.QrCode,
                            modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("QR Login")
                    }
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            LoginButtons(
                onSignInViaCredentials = { },
                onSignInViaQR = { },
            )
        }
    }
}