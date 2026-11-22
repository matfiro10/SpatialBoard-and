package com.example.spatialboard.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

class RoomConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val jsonArray = JSONArray()
        value.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val jsonArray = JSONArray(value)
        return List(jsonArray.length()) { index ->
            jsonArray.getString(index)
        }
    }
}