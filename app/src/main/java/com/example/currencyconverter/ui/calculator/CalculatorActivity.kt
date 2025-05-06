package com.example.currencyconverter.ui.calculator

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.currencyconverter.databinding.ActivityCalculatorBinding
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.operator.Operator

class CalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculatorBinding

    private var expression = "" // The text user is building
    private var snippet = ""    // Preview result

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.TRANSPARENT
// on Android 12+ to allow transparent nav bar:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.isNavigationBarContrastEnforced = false
        }

        // Load saved data from SharedPreferences
        val prefs = getSharedPreferences("calc_prefs", Context.MODE_PRIVATE)
        expression = prefs.getString("EXPRESSION", "") ?: ""
        snippet = prefs.getString("SNIPPET", "") ?: ""

        // Update UI
        showExpression(expression)
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
            val value = ExpressionBuilder(expr)
                .build()
                .evaluate()

            if (value % 1.0 == 0.0) value.toLong().toString()
            else value.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun showExpression(expr: String) {
        // 1) reset to the “natural” max size
        binding.tvExpression.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56f)

        // 2) assign the text
        binding.tvExpression.text = expr

        // 3) request a layout pass so auto-size has a chance to run again
        binding.tvExpression.post {
            binding.tvExpression.requestLayout()
        }

    }

}
