package com.example.currencyconverter.ui.calculator

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.currencyconverter.databinding.ActivityCalculatorBinding

class CalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculatorBinding

    private var expression = "" // The text user is building
    private var snippet = ""    // Preview result

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load saved data from SharedPreferences
        val prefs = getSharedPreferences("calc_prefs", Context.MODE_PRIVATE)
        expression = prefs.getString("EXPRESSION", "") ?: ""
        snippet = prefs.getString("SNIPPET", "") ?: ""

        // Update UI
        binding.tvExpression.text = expression
        binding.tvSnippet.text = snippet

        // Back to main screen
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Delete last character
        binding.btnDelete.setOnClickListener {
            if (expression.isNotEmpty()) {
                expression = expression.dropLast(1)
                binding.tvExpression.text = expression
                calculateSnippet()
            }
        }

        // Digits
        binding.btn0.setOnClickListener { appendSymbol("0") }
        binding.btn1.setOnClickListener { appendSymbol("1") }
        binding.btn2.setOnClickListener { appendSymbol("2") }
        binding.btn3.setOnClickListener { appendSymbol("3") }
        binding.btn4.setOnClickListener { appendSymbol("4") }
        binding.btn5.setOnClickListener { appendSymbol("5") }
        binding.btn6.setOnClickListener { appendSymbol("6") }
        binding.btn7.setOnClickListener { appendSymbol("7") }
        binding.btn8.setOnClickListener { appendSymbol("8") }
        binding.btn9.setOnClickListener { appendSymbol("9") }
        binding.btnDot.setOnClickListener { appendSymbol(".") }

        // Operators
        binding.btnPlus.setOnClickListener { appendSymbol("+") }
        binding.btnMinus.setOnClickListener { appendSymbol("-") }
        binding.btnMultiply.setOnClickListener { appendSymbol("*") }
        binding.btnDivide.setOnClickListener { appendSymbol("/") }

        // Equals
        binding.btnEqual.setOnClickListener {
            val result = evaluateExpression(expression)
            binding.tvExpression.text = result
            expression = result
            snippet = ""
            binding.tvSnippet.text = ""
        }
    }

    override fun onStop() {
        super.onStop()
        // Save to SharedPreferences
        val prefs = getSharedPreferences("calc_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("EXPRESSION", expression)
            putString("SNIPPET", snippet)
            apply() // or commit()
        }
    }

    private fun appendSymbol(symbol: String) {
        expression += symbol
        binding.tvExpression.text = expression
        calculateSnippet()
    }

    private fun calculateSnippet() {
        val result = evaluateExpression(expression)
        snippet = result
        binding.tvSnippet.text = if (expression.isNotEmpty()) snippet else ""
    }

    private fun evaluateExpression(expr: String): String {
        return try {
            val safeExpr = expr.replace("×", "*").replace("÷", "/")
            SimpleCalculator.evaluate(safeExpr).toString()
        } catch (e: Exception) {
            ""
        }
    }
}
