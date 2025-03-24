package com.example.domain

import javax.inject.Inject

class GetExchangeRatesUseCase @Inject constructor(
    private val repository: CurrencyRepository
) {
    suspend operator fun invoke(): ExchangeRates = repository.getExchangeRates()
}
