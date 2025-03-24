package com.example.domain

import javax.inject.Inject

class GetCurrencyListUseCase @Inject constructor(
    private val repository: CurrencyRepository
) {
    suspend operator fun invoke(): List<CurrencyInfo> = repository.getCurrencyList()
}
