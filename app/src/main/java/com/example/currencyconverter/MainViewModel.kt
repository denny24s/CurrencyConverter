package com.example.currencyconverter

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.ViewModel
import com.example.domain.ExchangeRates
import com.example.domain.GetCurrencyListUseCase
import com.example.domain.GetExchangeRatesUseCase
import com.example.domain.CurrencyInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getExchangeRatesUseCase: GetExchangeRatesUseCase,
    private val getCurrencyListUseCase: GetCurrencyListUseCase
) : ViewModel() {

    // Fetch exchange rates from API (or Room cache)
    val exchangeRatesLiveData: LiveData<ExchangeRates> = liveData {
        emit(getExchangeRatesUseCase())
    }

    // Fetch the list of all available currencies from API (or Room cache)
    val currencyListLiveData: LiveData<List<CurrencyInfo>> = liveData {
        emit(getCurrencyListUseCase())
    }
}
