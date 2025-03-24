package com.example.data
import javax.inject.Inject

interface CurrencyRepository {
    suspend fun getExchangeRates(): CurrencyRatesResponse
}

class CurrencyRepositoryImpl @Inject constructor(
    private val api: CurrencyApiService
) : CurrencyRepository {
    override suspend fun getExchangeRates(): CurrencyRatesResponse {
        return api.getRates()
    }
}
