package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRates(entity: ExchangeRatesEntity)

    @Query("SELECT * FROM exchange_rates WHERE base = :base LIMIT 1")
    suspend fun getExchangeRates(base: String = "EUR"): ExchangeRatesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

    @Query("SELECT * FROM currencies")
    suspend fun getCurrencies(): List<CurrencyEntity>
}
