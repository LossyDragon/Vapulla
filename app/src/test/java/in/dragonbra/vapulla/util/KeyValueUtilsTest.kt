package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.vapulla.db.entity.SteamApp
import java.util.Date
import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [generateSteamApp] maps the app info tree PICS sends to a [SteamApp]. */
class KeyValueUtilsTest {

    /** A node with a value, or with [children] when it is a section. */
    private fun node(name: String, value: String? = null, vararg children: KeyValue): KeyValue =
        KeyValue(name, value).also { it.children.addAll(children) }

    private fun section(name: String, vararg children: KeyValue) = node(name, null, *children)

    private val appInfo = section(
        "appinfo",
        node("appid", "440"),
        section(
            "common",
            node("name", "Team Fortress 2"),
            node("type", "Game"),
            node("oslist", "windows,linux"),
            node("releasestate", "released"),
            node("review_percentage", "93"),
            node("controller_support", "partial"),
            node("workshop_visible", "1"),
            section(
                "header_image",
                node("english", "header.jpg"),
                node("klingon", "header_klingon.jpg"),
            ),
        ),
        section(
            "extended",
            node("developer", "Valve"),
            node("isfreeapp", "1"),
            node("gamedir", "tf"),
        ),
        section("config", node("installdir", "Team Fortress 2")),
        section(
            "depots",
            node("baselanguages", "english"),
            section(
                "441",
                section("config", node("oslist", "windows"), node("osarch", "64")),
                section(
                    "manifests",
                    section(
                        "public",
                        node("gid", "123456789"),
                        node("size", "1000"),
                        node("download", "800"),
                    ),
                ),
            ),
            section(
                "branches",
                section("public", node("buildid", "5"), node("timeupdated", "1700000000")),
                section("beta", node("buildid", "6"), node("pwdrequired", "1")),
            ),
        ),
        section("ufs", node("quota", "1000"), node("maxnumfiles", "10")),
    )

    @Test
    fun `the common, extended and config sections fill the app's own fields`() {
        val app = appInfo.generateSteamApp()

        assertEquals(440, app.id)
        assertEquals("Team Fortress 2", app.name)
        assertEquals(SteamApp.AppType.game, app.type)
        assertEquals(EnumSet.of(SteamApp.OS.windows, SteamApp.OS.linux), app.osList)
        assertEquals(SteamApp.ReleaseState.released, app.releaseState)
        assertEquals(93.toByte(), app.reviewPercentage)
        assertEquals(SteamApp.ControllerSupport.partial, app.controllerSupport)
        assertTrue(app.workshopVisible)
        assertEquals("Valve", app.developer)
        assertTrue(app.isFreeApp)
        assertEquals("tf", app.gameDir)
        assertEquals("Team Fortress 2", app.installDir)
        assertTrue(app.hasCloudSaves)
    }

    @Test
    fun `an image in a language the app does not know is left out`() {
        val app = appInfo.generateSteamApp()

        assertEquals(mapOf(SteamApp.Language.english to "header.jpg"), app.headerImage)
    }

    @Test
    fun `only the numbered children of the depots section are depots`() {
        val depots = appInfo.generateSteamApp().depots

        assertEquals(setOf(441), depots.keys)
    }

    @Test
    fun `a depot carries its platform and its manifest for each branch`() {
        val depot = appInfo.generateSteamApp().depots.getValue(441)

        assertEquals(EnumSet.of(SteamApp.OS.windows), depot.osList)
        assertEquals(SteamApp.OSArch.Arch64, depot.osArch)
        assertEquals(
            SteamApp.ManifestInfo(name = "public", gid = 123456789, size = 1000, download = 800),
            depot.manifests["public"],
        )
        assertTrue(depot.encryptedManifests.isEmpty())
        // Not a DLC's depot and not shared from another app.
        assertEquals(Int.MAX_VALUE, depot.dlcAppId)
        assertEquals(Int.MAX_VALUE, depot.depotFromApp)
    }

    @Test
    fun `branches are read with their build, password flag and time in milliseconds`() {
        val branches = appInfo.generateSteamApp().branches

        assertEquals(
            SteamApp.BranchInfo("public", 5, false, Date(1_700_000_000_000)),
            branches["public"],
        )
        assertTrue(branches.getValue("beta").pwdRequired)
    }

    @Test
    fun `app info with nothing in it still gives an app`() {
        val app = KeyValue().generateSteamApp()

        assertEquals("", app.name)
        assertEquals(SteamApp.AppType.invalid, app.type)
        assertTrue(app.depots.isEmpty())
        assertFalse(app.hasCloudSaves)
    }
}
