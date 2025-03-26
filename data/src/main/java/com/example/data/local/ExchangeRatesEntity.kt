// ExchangeRatesEntity.kt
package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRatesEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val base: String,
    // We removed date.
    val rates: Map<String, Double>
)
