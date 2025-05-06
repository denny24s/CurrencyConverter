package com.example.currencyconverter.ui.info

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
// on Android 12+ to allow transparent nav bar:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.isNavigationBarContrastEnforced = false
        }

        // 1) Hook your custom back arrow:
        binding.ivBack.setOnClickListener {
            finish()
        }

        // 2) Populate content:
        binding.tvAppName.text   = getString(R.string.currency_converter)
        binding.tvInfoBody.text  = getString(R.string.app_info_long)
    }
}

