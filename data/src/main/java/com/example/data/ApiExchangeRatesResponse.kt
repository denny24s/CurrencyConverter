package com.example.data

data class ApiExchangeRatesResponse(
    val date: String,
    val base: String,
    val rates: Map<String, Double>
)
