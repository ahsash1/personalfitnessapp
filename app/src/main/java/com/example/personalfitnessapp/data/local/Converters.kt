package com.example.personalfitnessapp.data.local

import androidx.room.TypeConverter
import com.example.personalfitnessapp.data.model.Muscle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Room has no native collection support, so lists are stored as JSON text columns. */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(value)

    @TypeConverter
    fun fromMuscleList(value: List<Muscle>): String = Json.encodeToString(value.map { it.name })

    @TypeConverter
    fun toMuscleList(value: String): List<Muscle> =
        Json.decodeFromString<List<String>>(value).map { Muscle.valueOf(it) }
}
