package com.example.currencyconverter.ui.info

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import com.example.currencyconverter.R
import com.example.currencyconverter.databinding.ActivityInfoBinding

class InfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.isNavigationBarContrastEnforced = false
        }

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.tvAppName.text = getString(R.string.currency_converter)
        binding.tvInfoBody.text = getString(R.string.app_info_long)
        binding.tvInfoBody.text = HtmlCompat.fromHtml(
            getString(R.string.app_info_long),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
}

