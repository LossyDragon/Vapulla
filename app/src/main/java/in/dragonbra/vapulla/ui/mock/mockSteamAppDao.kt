package `in`.dragonbra.vapulla.ui.mock

import androidx.paging.PagingSource
import androidx.paging.PagingState
import `in`.dragonbra.vapulla.data.dao.SteamAppDao
import `in`.dragonbra.vapulla.data.entity.SteamApp

val mockSteamAppDao = object : SteamAppDao {
    override suspend fun insert(apps: SteamApp) {}
    override suspend fun insert(apps: List<SteamApp>) {}
    override suspend fun update(app: SteamApp) {}

    override fun getAllOwnedAppsPaged(
        appType: Int,
        query: String,
        invalidPkgId: Int,
    ): PagingSource<Int, SteamApp> {
        return object : PagingSource<Int, SteamApp>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SteamApp> {
                val mockApps = listOf(
                    SteamApp(id = 730, name = "Counter-Strike 2", type = SteamApp.AppType.game),
                    SteamApp(id = 570, name = "Dota 2", type = SteamApp.AppType.game),
                    SteamApp(id = 440, name = "Team Fortress 2", type = SteamApp.AppType.game),
                    SteamApp(id = 1091500, name = "Cyberpunk 2077", type = SteamApp.AppType.game),
                    SteamApp(
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

            override fun getRefreshKey(state: PagingState<Int, SteamApp>): Int? = null
        }
    }

    override suspend fun findApp(appId: Int): SteamApp? = null

    override fun findBlocking(appId: Int): SteamApp? = null
}
