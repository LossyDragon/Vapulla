package `in`.dragonbra.vapulla.db.entity

import `in`.dragonbra.vapulla.db.entity.SteamApp.AppType
import `in`.dragonbra.vapulla.db.entity.SteamApp.Language
import `in`.dragonbra.vapulla.db.entity.SteamApp.OS
import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Test

/** The enums of [SteamApp] as PICS writes them and as the database stores them. */
class SteamAppTest {

    @Test
    fun `a list of platforms is read whatever its case and spacing`() {
        assertEquals(EnumSet.of(OS.windows, OS.linux), OS.from("windows, Linux"))
        assertEquals(EnumSet.of(OS.macos), OS.from("macos"))
    }

    @Test
    fun `no list of platforms reads as none`() {
        assertEquals(EnumSet.of(OS.none), OS.from(null as String?))
        assertEquals(EnumSet.of(OS.none), OS.from(""))
    }

    @Test
    fun `a set of platforms is stored as one number and read back from it`() {
        val platforms = EnumSet.of(OS.windows, OS.linux)

        assertEquals(5, OS.code(platforms))
        assertEquals(platforms, OS.from(5))
        assertEquals(EnumSet.of(OS.none), OS.from(0))
    }

    @Test
    fun `an app type is read by its name or its number`() {
        assertEquals(AppType.game, AppType.from("Game"))
        assertEquals(AppType.dlc, AppType.fromCode(0x20))
        assertEquals(AppType.invalid, AppType.from("something new"))
        assertEquals(AppType.invalid, AppType.from(null))
    }

    @Test
    fun `several app types make one number of their bits`() {
        assertEquals(0x21, AppType.toFlags(EnumSet.of(AppType.game, AppType.dlc)))
    }

    @Test
    fun `a store asset with a content hash is on the shared host, a bare file on the old one`() {
        assertEquals(
            "https://shared.steamstatic.com/store_item_assets/steam/apps/440/abc123/header.jpg",
            steamStoreAssetUrl(440, "abc123/header.jpg"),
        )
        assertEquals(
            "https://cdn.akamai.steamstatic.com/steam/apps/440/header.jpg",
            steamStoreAssetUrl(440, "header.jpg"),
        )
    }

    @Test
    fun `the header image is the English one, or the plain file when PICS names none`() {
        val german = mapOf(Language.german to "de/header.jpg")
        val english = german + (Language.english to "en/header.jpg")

        assertEquals(steamStoreAssetUrl(440, "en/header.jpg"), steamHeaderImageUrl(440, english))
        assertEquals(steamStoreAssetUrl(440, "header.jpg"), steamHeaderImageUrl(440, german))
        assertEquals(steamStoreAssetUrl(440, "header.jpg"), SteamApp(id = 440).headerImageUrl)
    }

    @Test
    fun `cloud saves are on when PICS gives a quota or a file limit`() {
        assertEquals(false, SteamApp(id = 1).hasCloudSaves)
        assertEquals(true, SteamApp(id = 1, ufs = SteamApp.UFS(quota = 1)).hasCloudSaves)
        assertEquals(true, SteamApp(id = 1, ufs = SteamApp.UFS(maxNumFiles = 1)).hasCloudSaves)
    }
}
