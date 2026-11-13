package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.dao.SteamLicenseDao
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import java.text.DateFormat
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** An app a license grants. [name] is empty when the app's info was never fetched. */
@Immutable
data class LicensedApp(val id: Int, val name: String) {
    val label: String get() = name.ifEmpty { "App $id" }
}

/**
 * One package on the account, ready to show.
 *
 * @property apps every app the package grants.
 * @property matches the apps that matched the search, when it was an app that was searched for.
 */
@Immutable
data class LicenseRow(
    val packageId: Int,
    val type: String,
    val payment: String,
    val flags: String,
    val created: Long,
    val owners: Int,
    val depotCount: Int,
    val changeNumber: Int,
    val apps: ImmutableList<LicensedApp>,
    val matches: ImmutableList<LicensedApp> = persistentListOf(),
)

@Immutable
data class LicensesUiState(
    val query: String = "",
    val total: Int = 0,
    val licenses: ImmutableList<LicenseRow> = persistentListOf(),
    val isLoading: Boolean = true,
)

/**
 * The account's licenses as Steam sent them (`LicenseListCallback`), joined with the apps
 * each package grants from its PICS data. Searching for an app answers "why do I own this":
 * what is left is the packages that grant it.
 */
class LicensesViewModel(private val licenseDao: SteamLicenseDao, private val appDao: SteamAppDao) :
    ViewModel() {

    private val query = MutableStateFlow("")
    private val all = MutableStateFlow<List<LicenseRow>?>(null)

    val uiState: StateFlow<LicensesUiState> = combine(query, all) { query, all ->
        LicensesUiState(
            query = query,
            total = all?.size ?: 0,
            licenses = all.orEmpty().search(query.trim()).toImmutableList(),
            isLoading = all == null,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LicensesUiState(),
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val names = appDao.getAllNames().associate { it.id to it.name }

            all.value = licenseDao.getAllLicenses()
                .sortedByDescending { it.timeCreated }
                .map { license ->
                    LicenseRow(
                        packageId = license.packageID,
                        type = license.licenseType.name,
                        payment = license.paymentMethod.name,
                        flags = license.licenseFlags.joinToString { it.name },
                        created = license.timeCreated.time,
                        owners = license.ownerAccountID.size,
                        depotCount = license.depotIds.size,
                        changeNumber = license.lastChangeNumber,
                        apps = license.appIds
                            .map { LicensedApp(it, names[it].orEmpty()) }
                            .toImmutableList(),
                    )
                }
        }
    }

    fun search(text: String) {
        query.value = text
    }

    /** Keeps packages whose ID matches, or that grant an app whose ID or name matches. */
    private fun List<LicenseRow>.search(query: String): List<LicenseRow> {
        if (query.isEmpty()) return this

        return mapNotNull { row ->
            val matches = row.apps.filter { app ->
                app.id.toString() == query || app.name.contains(query, ignoreCase = true)
            }

            when {
                matches.isNotEmpty() -> row.copy(matches = matches.toImmutableList())
                row.packageId.toString() == query -> row
                else -> null
            }
        }
    }
}

@Composable
fun LicensesScreen(viewModel: LicensesViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LicensesContent(state = state, onBack = onBack, onSearch = viewModel::search)
}

@Composable
private fun LicensesContent(
    state: LicensesUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
) {
    // The field keeps its own text. Feeding it from the view model's state, which is worked
    // out on another thread, would hand it stale text mid-typing and scramble the input.
    var text by rememberSaveable { mutableStateOf(state.query) }

    UnderHoodScaffold(title = "Licenses", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(padding.exceptBottom()),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onSearch(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Game name, app ID or package ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                text = ""
                                onSearch("")
                            },
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
            )

            when {
                state.isLoading -> LoadingBox(modifier = Modifier.fillMaxSize())

                state.licenses.isEmpty() -> SearchResultMessage(
                    message = if (state.query.isBlank()) {
                        "No licenses yet: they arrive after signing in"
                    } else {
                        "No package on this account grants that"
                    },
                )

                else -> LicenseList(state = state, bottomPadding = padding)
            }
        }
    }
}

@Composable
private fun LicenseList(state: LicensesUiState, bottomPadding: PaddingValues) {
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listContentPadding(bottomPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "count") {
            Text(
                text = if (state.query.isBlank()) {
                    "${state.total} packages"
                } else {
                    "${state.licenses.size} of ${state.total} packages"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }

        itemsIndexed(state.licenses, key = { _, row -> row.packageId }) { index, row ->
            LicenseItem(
                row = row,
                expanded = expanded == row.packageId,
                shape = groupedListShape(index, state.licenses.size),
                onClick = { expanded = row.packageId.takeIf { it != expanded } },
            )
        }
    }
}

@Composable
private fun LicenseItem(row: LicenseRow, expanded: Boolean, shape: Shape, onClick: () -> Unit) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Package ${row.packageId}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            // What the package is for: the searched-for apps, or its first few.
            val shown = row.matches.ifEmpty { row.apps }.take(SUMMARY_APPS)
            if (shown.isNotEmpty()) {
                val more = row.apps.size - shown.size
                Text(
                    text = shown.joinToString { it.label } + if (more > 0) " +$more more" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                InfoPill(text = row.type)
                InfoPill(text = row.payment)
                if (row.created > 0) {
                    InfoPill(text = DateFormat.getDateInstance().format(Date(row.created)))
                }
                if (row.owners > 1) {
                    InfoPill(text = "${row.owners} owners")
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Detail("Flags", row.flags.ifEmpty { "None" })
                    Detail("Apps", row.apps.size.toString())
                    Detail("Depots", row.depotCount.toString())
                    Detail("PICS change number", row.changeNumber.toString())
                    if (row.apps.size > SUMMARY_APPS || row.matches.isNotEmpty()) {
                        Detail("All apps", row.apps.joinToString { "${it.label} (${it.id})" })
                    }
                }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val SUMMARY_APPS = 3

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    fun row(id: Int, vararg apps: Pair<Int, String>) = LicenseRow(
        packageId = id,
        type = "SinglePurchase",
        payment = "CreditCard",
        flags = "",
        created = 1_600_000_000_000,
        owners = 1,
        depotCount = 4,
        changeNumber = 123_456,
        apps = apps.map { LicensedApp(it.first, it.second) }.toImmutableList(),
    )

    VapullaTheme {
        LicensesContent(
            state = LicensesUiState(
                total = 553,
                licenses = persistentListOf(
                    row(469, 440 to "Team Fortress 2"),
                    row(1465, 220 to "Half-Life 2", 320 to "Half-Life 2: Deathmatch", 340 to ""),
                ),
                isLoading = false,
            ),
            onBack = {},
            onSearch = {},
        )
    }
}
