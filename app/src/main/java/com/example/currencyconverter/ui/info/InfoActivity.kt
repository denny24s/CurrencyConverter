package com.example.currencyconverter.ui.info

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.currencyconverter.R
import com.example.currencyconverter.databinding.ActivityInfoBinding

class InfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Hook your custom back arrow:
        binding.ivBack.setOnClickListener {
            finish()
        }

        // 2) Populate content:
        binding.tvAppName.text   = getString(R.string.currency_converter)
        binding.tvInfoBody.text  = getString(R.string.app_info_long)
    }
}

