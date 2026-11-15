package `in`.dragonbra.vapulla.db.converters

import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/** [ListTypeConverter] writes lists and maps into one text column and reads them back. */
class ListTypeConverterTest {

    private val converter = ListTypeConverter()

    @Test
    fun `a list of IDs is stored comma-separated`() {
        assertEquals("441,442,443", converter.fromIntList(listOf(441, 442, 443)))
        assertEquals(listOf(441, 442, 443), converter.toIntList("441,442,443"))
    }

    @Test
    fun `an empty list of IDs is stored as empty text`() {
        assertEquals("", converter.fromIntList(emptyList()))
        assertEquals(emptyList<Int>(), converter.toIntList(""))
    }

    @Test
    fun `text with a comma or a quote in it survives being stored`() {
        val names = persistentListOf("Gordon", "Freeman, G.", "the \"one\" free man")

        assertEquals(names, converter.toStringList(converter.fromStringList(names)))
    }

    @Test
    fun `a map is read back as it was stored`() {
        val presence = mapOf("status" to "In a match", "steam_display" to "#Status")

        assertEquals(presence, converter.toStringMap(converter.fromStringMap(presence)))
    }

    @Test
    fun `empty text reads as an empty map`() {
        assertEquals(emptyMap<String, String>(), converter.toStringMap(""))
    }
}
