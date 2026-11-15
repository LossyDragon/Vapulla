package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStoreSteamclient.CStore_GetLocalizedNameForTags_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStorebrowseSteamclient.CStoreBrowse_GetItems_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStorebrowseSteamclient.StoreBrowseContext
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStorebrowseSteamclient.StoreBrowseItemDataRequest
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStorebrowseSteamclient.StoreItem
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesStorebrowseSteamclient.StoreItemID
import `in`.dragonbra.javasteam.rpc.service.Store
import `in`.dragonbra.javasteam.rpc.service.StoreBrowse
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

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
                    .setLanguage(LANGUAGE)
                    .setCountryCode(country),
            )
            .setDataRequest(wanted)
            .build()

        val response = storeBrowse.getItems(request).await()
        if (response.result != EResult.OK) {
            Timber.w("StoreBrowse.GetItems failed: ${response.result}")
            return emptyList()
        }

        // An app the store does not know still comes back, marked as not found.
        return response.body.storeItemsList.filter { it.success == SUCCESS && it.visible }
    }

    /** The names of store tags, in the order of [tagIds]. Empty when Steam will not say. */
    private suspend fun tagNames(tagIds: List<Int>): List<String> {
        if (tagIds.isEmpty()) return emptyList()

        val request = CStore_GetLocalizedNameForTags_Request.newBuilder()
            .setLanguage(LANGUAGE)
            .addAllTagids(tagIds)
            .build()

        val response = store.getLocalizedNameForTags(request).await()
        if (response.result != EResult.OK) {
            Timber.w("Store.GetLocalizedNameForTags failed: ${response.result}")
            return emptyList()
        }

        val names = response.body.tagsList.associate { it.tagid to it.name }
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
        val original = option.originalPriceInCents
        val final = option.finalPriceInCents

        return when {
            option.discountPct > 0 -> option.formattedOriginalPrice to option.discountPct

            option.bundleDiscountPct > 0 ->
                option.formattedPriceBeforeBundleDiscount to option.bundleDiscountPct

            // Steam may leave the percentage out; the two prices still say it.
            original > final && final > 0 ->
                option.formattedOriginalPrice to ((original - final) * 100 / original).toInt()

            else -> "" to 0
        }
    }

    private fun StoreItem.toPage(tags: List<String>): StorePage {
        val reviews = this.reviews.summaryFiltered
        val (originalPrice, discountPercent) = discount()

        // File names are relative to a per-app folder that the item itself spells out.
        fun asset(file: String) = file.takeIf { it.isNotEmpty() }
            ?.let { ASSET_URL + assets.assetUrlFormat.replace(FILE_PLACEHOLDER, it) }
            .orEmpty()

        return StorePage(
            appId = appid,
            name = name,
            description = basicInfo.shortDescription,
            headerUrl = asset(assets.header),
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
        val found = withContext(Dispatchers.IO) {
            val address = SEARCH_URL +
                "?term=${URLEncoder.encode(term, "UTF-8")}" +
                "&l=$LANGUAGE&cc=${URLEncoder.encode(country, "UTF-8")}"
            val connection = URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            val text = try {
                connection.inputStream.use { it.readBytes().decodeToString() }
            } finally {
                connection.disconnect()
            }

            json.decodeFromString<SearchAnswer>(text).items.filter { it.type == "app" }
        }
        if (found.isEmpty()) return emptyList()

        // The website's search tells a free game and one not yet for sale apart badly, so
        // the prices are asked from the service the store page uses.
        val wanted = StoreBrowseItemDataRequest.newBuilder().setIncludeRelease(true)
        val priced = runCatching { items(found.map { it.id }, country, wanted) }
            .onFailure { Timber.w(it, "Prices for the store search failed") }
            .getOrDefault(emptyList())
            .associateBy { it.appid }

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
        val onSale = price.initial > price.final && price.final > 0

        return copy(
            price = Utils.formatMoney(price.final, price.currency),
            originalPrice = if (onSale) Utils.formatMoney(price.initial, price.currency) else "",
            discountPercent = if (onSale) {
                ((price.initial - price.final) * 100 / price.initial).toInt()
            } else {
                0
            },
        )
    }

    private companion object {
        const val LANGUAGE = "english"
        const val TAG_COUNT = 8

        /** What `success` holds for an item the store found (an `EResult.OK`). */
        const val SUCCESS = 1

        const val STORE_URL = "https://store.steampowered.com/"
        const val SEARCH_URL = "https://store.steampowered.com/api/storesearch/"
        const val ASSET_URL = "https://shared.akamai.steamstatic.com/store_item_assets/"
        const val FILE_PLACEHOLDER = "\${FILENAME}"
        const val TIMEOUT_MS = 10_000

        val json = Json { ignoreUnknownKeys = true }
    }
}
