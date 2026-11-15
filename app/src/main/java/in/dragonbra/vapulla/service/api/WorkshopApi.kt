package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPublishedfileSteamclient.CPublishedFile_QueryFiles_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPublishedfileSteamclient.CPublishedFile_Subscribe_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPublishedfileSteamclient.CPublishedFile_Unsubscribe_Request
import `in`.dragonbra.javasteam.rpc.service.PublishedFile
import timber.log.Timber

/** Workshop browsing and subscriptions via the `PublishedFile` unified service. */
class WorkshopApi(private val publishedFile: PublishedFile) {

    /** One page of items for [appId]; pages start at 1. Empty when Steam does not answer. */
    suspend fun query(
        appId: Int,
        sort: WorkshopSort,
        page: Int = 1,
        searchText: String? = null,
        perPage: Int = PAGE_SIZE,
    ): Page<WorkshopItem> {
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

        val body = publishedFile.queryFiles(request).bodyOrNull("QueryFiles") ?: return Page.empty()
        Timber.d(
            "QueryFiles page=$page total=${body.total} items=${body.publishedfiledetailsCount}",
        )

        val items = body.publishedfiledetailsList.map { details ->
            WorkshopItem(
                id = details.publishedfileid,
                title = details.title,
                description = details.shortDescription,
                previewUrl = details.previewUrl,
                creator = details.creator,
                fileSize = details.fileSize,
                timeUpdated = details.timeUpdated.toUInt().toLong(),
                subscriptions = details.subscriptions,
                favorited = details.favorited,
            )
        }

        return Page.of(items, page, perPage, body.total)
    }

    /** Subscribes the account to the item [fileId] of [appId]. */
    suspend fun subscribe(appId: Int, fileId: Long): EResult {
        val request = CPublishedFile_Subscribe_Request.newBuilder()
            .setPublishedfileid(fileId)
            .setListType(LIST_TYPE_SUBSCRIPTIONS)
            .setAppid(appId)
            .setNotifyClient(true)
            .build()
        return publishedFile.subscribe(request).resultOf("Subscribe")
    }

    /** Ends the account's subscription to the item [fileId] of [appId]. */
    suspend fun unsubscribe(appId: Int, fileId: Long): EResult {
        val request = CPublishedFile_Unsubscribe_Request.newBuilder()
            .setPublishedfileid(fileId)
            .setListType(LIST_TYPE_SUBSCRIPTIONS)
            .setAppid(appId)
            .setNotifyClient(true)
            .build()
        return publishedFile.unsubscribe(request).resultOf("Unsubscribe")
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val TREND_DAYS = 7

        /** Steam's list type for the items an account is subscribed to. */
        const val LIST_TYPE_SUBSCRIPTIONS = 1
    }
}
