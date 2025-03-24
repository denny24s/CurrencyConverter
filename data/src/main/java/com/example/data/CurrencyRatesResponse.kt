package com.example.data

data class CurrencyRatesResponse(
    val date: String,
    val base: String,
    val rates: Map<String, Double>
)
