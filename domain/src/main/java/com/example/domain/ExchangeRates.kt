package com.example.domain

data class ExchangeRates(
    val date: String,
    val base: String,
    val rates: Map<String, Double>
)

data class CurrencyInfo(
    val code: String,
    val name: String
)

interface CurrencyRepository {
    suspend fun getExchangeRates(): ExchangeRates
    suspend fun getCurrencyList(): List<CurrencyInfo>
}
