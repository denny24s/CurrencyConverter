// CurrencyApi.kt
package com.example.data

import retrofit2.http.GET

interface CurrencyApi {
    @GET("v1/currencies/eur.json")
    suspend fun getEurRates(): ApiExchangeRatesResponse

    @GET("v1/currencies.json")
    suspend fun getAllCurrencies(): ApiCurrenciesResponse
}
