package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStoreSteamclient.CStore_GetLocalizedNameForTags_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStorebrowseSteamclient.CStoreBrowse_GetItems_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStorebrowseSteamclient.StoreBrowseContext
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStorebrowseSteamclient.StoreBrowseItemDataRequest
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStorebrowseSteamclient.StoreItem
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStorebrowseSteamclient.StoreItemID
import `in`.dragonbra.javasteam.rpc.service.Store
import `in`.dragonbra.javasteam.rpc.service.StoreBrowse
import `in`.dragonbra.vapulla.util.LenientJson
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import `in`.dragonbra.vapulla.util.httpGetText
import java.net.URLEncoder
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Steam store, for any app whether the account owns it or not. A store page comes from
 * the `StoreBrowse` unified service and its tag names from `Store`. Those take app IDs, so
 * finding a game by name goes through the store website's own search; the prices of what it
 * finds are then asked from `StoreBrowse`, so that a search and a page always agree.
 *
 * Prices depend on where the buyer is, so both calls take the account's country.
 */
class StoreApi(private val storeBrowse: StoreBrowse, private val store: Store) {

    /** The store page of [appId] as [country] sees it. Null when the store has none. */
    suspend fun page(appId: Int, country: String): StorePage? {
        val wanted = StoreBrowseItemDataRequest.newBuilder()
            .setIncludeBasicInfo(true)
            .setIncludeAssets(true)
            .setIncludeRelease(true)
            .setIncludePlatforms(true)
            .setIncludeReviews(true)
            .setIncludeScreenshots(true)
            .setIncludeTagCount(TAG_COUNT)

        val item = items(listOf(appId), country, wanted).firstOrNull() ?: return null

        return item.toPage(tagNames(item.tagidsList))
    }

    /**
     * [appIds] as rows of a list, in that order, priced for [country]: what a wishlist is
     * shown as. Apps the store no longer shows are left out; empty when Steam does not answer.
     */
    suspend fun listing(appIds: List<Int>, country: String): List<StoreSearchResult> {
        val wanted = StoreBrowseItemDataRequest.newBuilder()
            .setIncludeAssets(true)
            .setIncludeRelease(true)

        val found = appIds.chunked(ITEMS_PER_REQUEST)
            .flatMap { items(it, country, wanted) }
            .associateBy { it.appid }

        return appIds.mapNotNull { appId ->
            found[appId]?.let { item ->
                StoreSearchResult(
                    appId = appId,
                    name = item.name,
                    imageUrl = item.assetUrl(item.assets.smallCapsule),
                    price = "",
                ).pricedByStore(item)
            }
        }
    }

    /**
     * What the store holds on [appIds] as [country] sees it, with the parts asked for in
     * [wanted]. Apps the store does not know or does not show are left out; empty when
     * Steam does not answer.
     */
    private suspend fun items(
        appIds: List<Int>,
        country: String,
        wanted: StoreBrowseItemDataRequest.Builder,
    ): List<StoreItem> {
        val request = CStoreBrowse_GetItems_Request.newBuilder()
            .addAllIds(appIds.map { StoreItemID.newBuilder().setAppid(it).build() })
            .setContext(
                StoreBrowseContext.newBuilder()
                    .setLanguage(STEAM_LANGUAGE)
                    .setCountryCode(country),
            )
            .setDataRequest(wanted)
            .build()

        val body = storeBrowse.getItems(request).bodyOrNull("StoreBrowse.GetItems")
            ?: return emptyList()

        // An app the store does not know still comes back, marked as not found.
        return body.storeItemsList.filter { it.success == SUCCESS && it.visible }
    }

    /** The names of store tags, in the order of [tagIds]. Empty when Steam will not say. */
    private suspend fun tagNames(tagIds: List<Int>): List<String> {
        if (tagIds.isEmpty()) return emptyList()

        val request = CStore_GetLocalizedNameForTags_Request.newBuilder()
            .setLanguage(STEAM_LANGUAGE)
            .addAllTagids(tagIds)
            .build()

        val body = store.getLocalizedNameForTags(request)
            .bodyOrNull("Store.GetLocalizedNameForTags")
            ?: return emptyList()

        val names = body.tagsList.associate { it.tagid to it.name }
        return tagIds.mapNotNull { names[it]?.takeIf(String::isNotEmpty) }
    }

    /** True for an app the store lists ahead of its release. */
    private val StoreItem.comingSoon: Boolean get() = release.isComingSoon || isComingSoon

    /**
     * The price before a discount and the discount in percent, or "" and 0 when the app is
     * not on sale. A discount can be the app's own or that of the bundle it is sold in.
     */
    private fun StoreItem.discount(): Pair<String, Int> {
        val option = bestPurchaseOption
        // Steam may leave the percentage out; the two prices still say it.
        val fromPrices = discountPercent(option.originalPriceInCents, option.finalPriceInCents)

        return when {
            option.discountPct > 0 -> option.formattedOriginalPrice to option.discountPct

            option.bundleDiscountPct > 0 ->
                option.formattedPriceBeforeBundleDiscount to option.bundleDiscountPct

            fromPrices > 0 -> option.formattedOriginalPrice to fromPrices

            else -> "" to 0
        }
    }

    /** How much lower [final] is than [original], in percent; 0 when it is not a discount. */
    private fun discountPercent(original: Long, final: Long): Int =
        if (final in 1..<original) ((original - final) * 100 / original).toInt() else 0

    /**
     * The address of one of the item's pictures; empty when it has no such picture. File
     * names are relative to a per-app folder that the item itself spells out.
     */
    private fun StoreItem.assetUrl(file: String): String = file.takeIf { it.isNotEmpty() }
        ?.let { ASSET_URL + assets.assetUrlFormat.replace(FILE_PLACEHOLDER, it) }
        .orEmpty()

    private fun StoreItem.toPage(tags: List<String>): StorePage {
        val reviews = this.reviews.summaryFiltered
        val (originalPrice, discountPercent) = discount()

        return StorePage(
            appId = appid,
            name = name,
            description = basicInfo.shortDescription,
            headerUrl = assetUrl(assets.header),
            developers = basicInfo.developersList.joinToString { it.name },
            publishers = basicInfo.publishersList.joinToString { it.name },
            isFree = isFree,
            price = bestPurchaseOption.formattedFinalPrice,
            originalPrice = originalPrice,
            discountPercent = discountPercent,
            reviewLabel = reviews.reviewScoreLabel,
            reviewPercent = reviews.percentPositive,
            reviewCount = reviews.reviewCount,
            releaseDate = release.steamReleaseDate.toUInt().toLong(),
            isComingSoon = comingSoon,
            isEarlyAccess = isEarlyAccess,
            windows = platforms.windows,
            mac = platforms.mac,
            linux = platforms.steamosLinux,
            tags = tags.toImmutableList(),
            screenshots = screenshots.allAgesScreenshotsList
                .sortedBy { it.ordinal }
                .map { ASSET_URL + it.filename }
                .toImmutableList(),
            storeUrl = STORE_URL + storeUrlPath,
        )
    }

    @Serializable
    private class SearchPrice(val currency: String = "", val initial: Long = 0, val final: Long = 0)

    @Serializable
    private class SearchItem(
        val type: String = "",
        val id: Int = 0,
        val name: String = "",
        val price: SearchPrice? = null,
        @SerialName("tiny_image") val image: String = "",
    )

    @Serializable
    private class SearchAnswer(val items: List<SearchItem> = emptyList())

    /**
     * Store apps whose name matches [term], best match first, priced for [country].
     *
     * @throws java.io.IOException if the store cannot be reached.
     */
    suspend fun search(term: String, country: String): List<StoreSearchResult> {
        val address = SEARCH_URL +
            "?term=${encode(term)}&l=$STEAM_LANGUAGE&cc=${encode(country)}"
        val found = LenientJson.decodeFromString<SearchAnswer>(httpGetText(address)).items
            .filter { it.type == "app" }
        if (found.isEmpty()) return emptyList()

        // The website's search tells a free game and one not yet for sale apart badly, so
        // the prices are asked from the service the store page uses. Without an answer the
        // website's own prices are shown.
        val wanted = StoreBrowseItemDataRequest.newBuilder().setIncludeRelease(true)
        val priced = items(found.map { it.id }, country, wanted).associateBy { it.appid }

        return found.map { item ->
            val result = StoreSearchResult(
                appId = item.id,
                name = item.name.decodeHtml(),
                imageUrl = item.image,
                price = "",
            )
            priced[item.id]?.let { result.pricedByStore(it) } ?: result.pricedByWebsite(item.price)
        }
    }

    /** [text] made safe to put in a query string. */
    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8")

    private fun StoreSearchResult.pricedByStore(item: StoreItem): StoreSearchResult {
        val (originalPrice, discountPercent) = item.discount()
        val price = item.bestPurchaseOption.formattedFinalPrice

        return copy(
            price = when {
                item.isFree -> "Free"
                price.isNotEmpty() -> price
                item.comingSoon -> "Coming soon"
                else -> "Not for sale"
            },
            originalPrice = originalPrice,
            discountPercent = discountPercent,
        )
    }

    /** The website's own price, for when the service did not answer; it may have none. */
    private fun StoreSearchResult.pricedByWebsite(price: SearchPrice?): StoreSearchResult {
        if (price == null) return this
        val percent = discountPercent(price.initial, price.final)
        val original = if (percent > 0) Utils.formatMoney(price.initial, price.currency) else ""

        return copy(
            price = Utils.formatMoney(price.final, price.currency),
            originalPrice = original,
            discountPercent = percent,
        )
    }

    private companion object {
        const val TAG_COUNT = 8

        /** How many apps one `GetItems` call is asked about. */
        const val ITEMS_PER_REQUEST = 50

        /** What `success` holds for an item the store found (an `EResult.OK`). */
        const val SUCCESS = 1

        const val STORE_URL = "https://store.steampowered.com/"
        const val SEARCH_URL = "https://store.steampowered.com/api/storesearch/"
        const val ASSET_URL = "https://shared.akamai.steamstatic.com/store_item_assets/"
        const val FILE_PLACEHOLDER = $$"${FILENAME}"
    }
}
