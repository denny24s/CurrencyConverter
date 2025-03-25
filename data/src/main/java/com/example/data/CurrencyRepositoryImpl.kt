package com.example.data

import com.example.data.local.CurrencyDao
import com.example.data.local.CurrencyEntity
import com.example.data.local.ExchangeRatesEntity
import com.example.domain.CurrencyInfo
import com.example.domain.CurrencyRepository
import com.example.domain.ExchangeRates
import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor(
    private val api: CurrencyApiService,
    private val dao: CurrencyDao
) : CurrencyRepository {
    override suspend fun getExchangeRates(): ExchangeRates {
        // Fetch exchange rates from API.
        val apiResponse = api.getRates()
        val baseValue = apiResponse.base ?: "EUR"
        val dateValue = apiResponse.date ?: ""
        val ratesValue = apiResponse.rates ?: emptyMap()

        // Save to Room.
        val entity = ExchangeRatesEntity(
            base = baseValue,
            date = dateValue,
            rates = ratesValue
        )
        dao.insertExchangeRates(entity)
        // Convert API response to domain model.
        return ExchangeRates(
            date = dateValue,
            base = baseValue,
            rates = ratesValue
        )
    }

    override suspend fun getCurrencyList(): List<CurrencyInfo> {
        // Fetch currencies from API.
        val apiResponse = api.getCurrencies()
        // Provide a default empty map if currencies is null.
        val entities = (apiResponse.currencies ?: emptyMap()).map { (code, name) ->
            CurrencyEntity(code.uppercase(), name)
        }
        dao.insertCurrencies(entities)
        return entities.map { CurrencyInfo(it.code, it.name) }
    }
}
