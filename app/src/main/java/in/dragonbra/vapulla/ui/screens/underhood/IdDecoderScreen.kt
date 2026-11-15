package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.FabClearance
import `in`.dragonbra.vapulla.ui.composables.Hint
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.DecodedId
import `in`.dragonbra.vapulla.util.IdDecoder
import java.io.IOException
import timber.log.Timber

/**
 * Takes one of Steam's IDs and shows what is packed into it, using JavaSteam's `SteamID`,
 * `GameID` and `GlobalID`. A bare number is shown every way it could be read, and a profile's
 * custom URL is looked up on the community site.
 *
 * Nothing is decoded or looked up while typing, only when the search button is pressed.
 */
@Composable
fun IdDecoderScreen(onBack: () -> Unit, initialInput: String = "") {
    var input by rememberSaveable { mutableStateOf(initialInput) }

    // What was last searched for, and how often: searching the same text again asks Steam again.
    var query by rememberSaveable { mutableStateOf(initialInput) }
    var searches by rememberSaveable { mutableIntStateOf(0) }
    val keyboard = LocalSoftwareKeyboardController.current

    val decoded = remember(query) { IdDecoder.decode(query) }

    // Anything that is not an ID might be a profile's custom URL, which only Steam can resolve.
    val customUrl = remember(query) {
        if (decoded.isEmpty()) IdDecoder.customUrlName(query) else null
    }
    val lookup by produceState<Lookup>(Lookup.Loading, customUrl, searches) {
        value = Lookup.Loading
        if (customUrl == null) return@produceState
        value = try {
            Lookup.Done(IdDecoder.resolveCustomUrl(customUrl))
        } catch (e: IOException) {
            Timber.w(e, "Custom URL lookup failed")
            Lookup.Failed
        }
    }

    UnderHoodScaffold(
        title = "ID Decoder",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    query = input
                    searches++
                    keyboard?.hide()
                },
                // The scaffold already keeps the button clear of the navigation bar, so only
                // the rest of the keyboard's height is added.
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.ime.exclude(WindowInsets.navigationBars),
                ),
            ) {
                Icon(Icons.Default.Search, contentDescription = "Decode")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom())
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(listContentPadding(padding, extraBottom = FabClearance)),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ID") },
                placeholder = { Text("Steam ID or custom URL", maxLines = 1) },
                singleLine = true,
                // No capitals from the keyboard: custom URLs are lower case, and in a Steam3 ID
                // the case of the letter is part of its meaning.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                trailingIcon = {
                    if (input.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                input = ""
                                query = ""
                            },
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
            )

            when {
                query.isBlank() -> Hint(
                    "Enter a 64-bit ID (76561197960287930), a Steam3 ID ([U:1:22202]), a " +
                        "Steam2 ID (STEAM_0:0:11101), a plain account or app ID, or a " +
                        "profile's custom URL, then press the search button.",
                )

                decoded.isNotEmpty() -> decoded.forEach { Reading(it) }

                customUrl == null -> Hint(
                    "That is not a number, a Steam ID in text form or a custom URL.",
                )

                else -> when (val result = lookup) {
                    Lookup.Loading -> Hint("Looking up the custom URL \"$customUrl\"…")

                    Lookup.Failed -> Hint("Could not reach Steam to look up \"$customUrl\".")

                    is Lookup.Done -> if (result.reading != null) {
                        Reading(result.reading)
                    } else {
                        Hint("No profile uses the custom URL \"$customUrl\".")
                    }
                }
            }
        }
    }
}

/** Where the lookup of a custom URL stands; [Done] holds null when no profile uses it. */
private sealed interface Lookup {
    data object Loading : Lookup
    data object Failed : Lookup
    data class Done(val reading: DecodedId?) : Lookup
}

@Composable
private fun Reading(reading: DecodedId) {
    Section(title = reading.title) {
        Hint(reading.note)
        DetailList(reading.rows)
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        IdDecoderScreen(onBack = {}, initialInput = "76561197960287930")
    }
}
