package com.example.data

import retrofit2.http.GET

interface CurrencyApiService {
    @GET("v1/currencies/eur.json")
    suspend fun getRates(): ApiExchangeRatesResponse

    @GET("v1/currencies.json")
    suspend fun getCurrencies(): ApiCurrenciesResponse
}
