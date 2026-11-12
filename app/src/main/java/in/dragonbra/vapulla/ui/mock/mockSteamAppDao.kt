package `in`.dragonbra.vapulla.ui.mock

import androidx.paging.PagingSource
import androidx.paging.PagingState
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamAppChangeInfo
import `in`.dragonbra.vapulla.db.entity.SteamAppListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

val mockSteamAppDao = object : SteamAppDao {
    override suspend fun insert(apps: SteamApp) {}
    override suspend fun insert(apps: List<SteamApp>) {}
    override suspend fun insertIfAbsent(apps: List<SteamApp>) {}
    override suspend fun update(app: SteamApp) {}
    override suspend fun updatePackageId(packageId: Int, appIds: List<Int>) {}
    override suspend fun updatePlaytime(appId: Int, forever: Int, twoWeeks: Int, lastPlayed: Long) {}
    override fun findAppFlow(appId: Int): Flow<SteamApp?> = flowOf(null)

    override fun getAllOwnedAppsPaged(
        appType: Int,
        query: String,
        invalidPkgId: Int,
    ): PagingSource<Int, SteamAppListItem> {
        return object : PagingSource<Int, SteamAppListItem>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SteamAppListItem> {
                val mockApps = listOf(
                    SteamAppListItem(
                        id = 730,
                        name = "Counter-Strike 2",
                        type = SteamApp.AppType.game,
                    ),
                    SteamAppListItem(id = 570, name = "Dota 2", type = SteamApp.AppType.game),
                    SteamAppListItem(
                        id = 440,
                        name = "Team Fortress 2",
                        type = SteamApp.AppType.game,
                    ),
                    SteamAppListItem(
                        id = 1091500,
                        name = "Cyberpunk 2077",
                        type = SteamApp.AppType.game,
                    ),
                    SteamAppListItem(
                        id = 1174180,
                        name = "Red Dead Redemption 2",
                        type = SteamApp.AppType.game,
                    ),
                ).filter {
                    query.isEmpty() || it.name.contains(query, ignoreCase = true)
                }

                return LoadResult.Page(
                    data = mockApps,
                    prevKey = null,
                    nextKey = null,
                )
            }

            override fun getRefreshKey(state: PagingState<Int, SteamAppListItem>): Int? = null
        }
    }

    override fun findServerGames(
        query: String,
        gameType: Int,
        invalidPkgId: Int,
    ): Flow<List<SteamAppListItem>> = flowOf(emptyList())

    override suspend fun findAppsAwaitingPics(invalidPkgId: Int): List<Int> = emptyList()

    override suspend fun findApp(appId: Int): SteamApp? = null

    override suspend fun findAppName(appId: Int): String? = null

    override suspend fun findChangeInfo(appIds: List<Int>): List<SteamAppChangeInfo> = emptyList()

    override fun findBlocking(appId: Int): SteamApp? = null
}
