package com.example.spatialboard.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomConvertersTest {

    private val converters = RoomConverters()

    @Test
    fun roundTripPreservesStandardAndSpecialCharacterTags() {
        val original = listOf(
            "Laboratorium",
            "Sala \"A-12\" [Piętro 2]",
            "ąęćłńóśźż, tag z przecinkiem",
            ""
        )

        val serialized = converters.fromStringList(original)
        val deserialized = converters.toStringList(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun emptyListSerializesAndDeserializesCleanly() {
        val serialized = converters.fromStringList(emptyList())
        val deserialized = converters.toStringList(serialized)

        assertEquals(emptyList<String>(), deserialized)
    }

    @Test
    fun blankStringReturnsEmptyList() {
        assertEquals(emptyList<String>(), converters.toStringList(""))
        assertEquals(emptyList<String>(), converters.toStringList("   "))
    }
}
