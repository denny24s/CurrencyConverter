package com.example.currencyconverter

object SimpleCalculator {
    fun evaluate(expression: String): Double {
        // Very simplistic approach—only for demonstration!
        // You might parse tokens or use a real library (e.g., exp4j).
        return when {
            expression.contains("+") -> {
                val parts = expression.split("+")
                parts[0].toDouble() + parts[1].toDouble()
            }
            expression.contains("-") -> {
                val parts = expression.split("-")
                parts[0].toDouble() - parts[1].toDouble()
            }
            expression.contains("*") -> {
                val parts = expression.split("*")
                parts[0].toDouble() * parts[1].toDouble()
            }
            expression.contains("/") -> {
                val parts = expression.split("/")
                parts[0].toDouble() / parts[1].toDouble()
            }
            else -> expression.toDoubleOrNull() ?: 0.0
        }
    }
}
