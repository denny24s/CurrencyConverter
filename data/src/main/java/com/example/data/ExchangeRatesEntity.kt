package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRatesEntity(
    @PrimaryKey val base: String, // e.g., "EUR"
    val date: String,
    val rates: Map<String, Double>
)
