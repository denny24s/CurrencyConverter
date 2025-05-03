package data.remote.api

import data.remote.dto.EurResponse
import retrofit2.http.GET

interface CurrencyApi {
    @GET("v1/currencies.json")
    suspend fun getAllCurrencies(): Map<String, String> // e.g. key: "USD", value: "United States Dollar"

    @GET("v1/currencies/eur.json")
    suspend fun getEurRates(): EurResponse
}
