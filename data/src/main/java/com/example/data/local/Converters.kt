package com.example.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromRatesMap(value: Map<String, Double>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toRatesMap(value: String): Map<String, Double> {
        val mapType = object : TypeToken<Map<String, Double>>() {}.type
        return Gson().fromJson(value, mapType)
    }
}
