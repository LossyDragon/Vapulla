package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPublishedfileSteamclient.CPublishedFile_QueryFiles_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPublishedfileSteamclient.CPublishedFile_Subscribe_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPublishedfileSteamclient.CPublishedFile_Unsubscribe_Request
import `in`.dragonbra.javasteam.rpc.service.PublishedFile
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/** Workshop browsing and subscriptions via the `PublishedFile` unified service. */
class WorkshopApi(private val publishedFile: PublishedFile) {

    /** One page of items for [appId]; pages start at 1. */
    suspend fun query(
        appId: Int,
        sort: WorkshopSort,
        page: Int = 1,
        searchText: String? = null,
        perPage: Int = PAGE_SIZE,
    ): WorkshopPage {
        val request = CPublishedFile_QueryFiles_Request.newBuilder()
            .setQueryType(sort.queryType)
            .setAppid(appId)
            .setPage(page)
            .setNumperpage(perPage)
            .setReturnShortDescription(true)
            .setReturnPreviews(true)
            .setReturnMetadata(true)
            .setReturnDetails(true)
            .apply {
                if (sort == WorkshopSort.Trending) days = TREND_DAYS
                if (!searchText.isNullOrBlank()) setSearchText(searchText)
            }
            .build()

        val response = publishedFile.queryFiles(request).await()
        if (response.result != EResult.OK) {
            Timber.w("QueryFiles failed: ${response.result}")
            return WorkshopPage(persistentListOf(), 0, null)
        }
        Timber.d(
            "QueryFiles page=$page total=${response.body.total} items=${response.body.publishedfiledetailsCount}",
        )

        val items = response.body.publishedfiledetailsList.map { details ->
            WorkshopItem(
                id = details.publishedfileid,
                title = details.title,
                description = details.shortDescription,
                previewUrl = details.previewUrl,
                creator = details.creator,
                fileSize = details.fileSize,
                timeUpdated = details.timeUpdated.toLong(),
                subscriptions = details.subscriptions,
                favorited = details.favorited,
            )
        }

        val seenSoFar = page * perPage
        return WorkshopPage(
            items = items.toImmutableList(),
            total = response.body.total,
            nextPage = (page + 1).takeIf { items.isNotEmpty() && seenSoFar < response.body.total },
        )
    }

    suspend fun subscribe(appId: Int, fileId: Long): EResult {
        val request = CPublishedFile_Subscribe_Request.newBuilder()
            .setPublishedfileid(fileId)
            .setListType(LIST_TYPE_SUBSCRIPTIONS)
            .setAppid(appId)
            .setNotifyClient(true)
            .build()
        return publishedFile.subscribe(request).await().result
    }

    suspend fun unsubscribe(appId: Int, fileId: Long): EResult {
        val request = CPublishedFile_Unsubscribe_Request.newBuilder()
            .setPublishedfileid(fileId)
            .setListType(LIST_TYPE_SUBSCRIPTIONS)
            .setAppid(appId)
            .setNotifyClient(true)
            .build()
        return publishedFile.unsubscribe(request).await().result
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val TREND_DAYS = 7
        const val LIST_TYPE_SUBSCRIPTIONS = 1
    }
}
