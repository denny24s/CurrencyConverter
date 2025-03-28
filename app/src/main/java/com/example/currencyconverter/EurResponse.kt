package com.example.currencyconverter

data class EurResponse(
    val date: String,
    val eur: Map<String, Double> // e.g. eur["USD"] = 1.12
)
