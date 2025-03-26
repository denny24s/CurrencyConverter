package com.example.data

data class ApiExchangeRatesResponse(
    val base: String,              // e.g. "EUR"
    val rates: Map<String, Double> // All currency rates relative to EUR.
)