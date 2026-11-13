package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.IdDecoder

/**
 * Takes one of Steam's IDs and shows what is packed into it, using JavaSteam's `SteamID`,
 * `GameID` and `GlobalID`. A bare number is shown every way it could be read.
 */
@Composable
fun IdDecoderScreen(onBack: () -> Unit, initialInput: String = "") {
    var input by rememberSaveable { mutableStateOf(initialInput) }
    val decoded = remember(input) { IdDecoder.decode(input) }

    UnderHoodScaffold(title = "ID Decoder", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom())
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(listContentPadding(padding)),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ID") },
                placeholder = { Text("76561197960287930, [U:1:22202] or STEAM_0:0:11101") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                ),
                trailingIcon = {
                    if (input.isNotEmpty()) {
                        IconButton(onClick = { input = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
            )

            when {
                input.isBlank() -> Hint(
                    "Enter a 64-bit ID from a profile link or a log line, a Steam2 or Steam3 " +
                        "ID, or a plain account or app ID.",
                )

                decoded.isEmpty() -> Hint("That is not a number or a Steam ID in text form.")

                else -> decoded.forEach { reading ->
                    Section(title = reading.title) {
                        Hint(reading.note)
                        DetailList(reading.rows)
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        IdDecoderScreen(onBack = {}, initialInput = "76561197960287930")
    }
}
