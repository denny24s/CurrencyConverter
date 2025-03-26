// CurrencyRepositoryImpl.kt
package com.example.data

import com.example.data.local.CurrencyDao
import com.example.data.local.CurrencyEntity
import com.example.data.local.ExchangeRatesEntity
import com.example.domain.CurrencyInfo
import com.example.domain.CurrencyRepository
import com.example.domain.ExchangeRates
import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor(
    private val api: CurrencyApi,
    private val dao: CurrencyDao
) : CurrencyRepository {

    override suspend fun getExchangeRates(): ExchangeRates {
        val apiResponse = api.getEurRates()
        // Save the rates to Room (if needed).
        val entity = ExchangeRatesEntity(
            base = apiResponse.base,
            // date removed—if you have a column for date you can omit or provide a default.
            rates = apiResponse.rates
        )
        dao.insertExchangeRates(entity)
        return ExchangeRates(
            base = apiResponse.base,
            rates = apiResponse.rates
        )
    }

    override suspend fun getCurrencyList(): List<CurrencyInfo> {
        val apiResponse = api.getAllCurrencies()
        // Ensure we have a non-null map.
        val currenciesMap = apiResponse.currencies ?: emptyMap()
        val entities = currenciesMap.map { (code, name) ->
            CurrencyEntity(code.uppercase(), name)
        }
        dao.insertCurrencies(entities)
        return entities.map { CurrencyInfo(it.code, it.name) }
    }
}
