package com.example.currencyconverter

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.currencyconverter.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Our main list adapter – initially with an empty list.
    private lateinit var adapter: CurrencyAdapter

    // Retrofit API instance.
    private val api: CurrencyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApi::class.java)
    }

    // We'll keep the full list of currencies (from API) here.
    private var fullCurrencyList: List<CurrencyInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up RecyclerView with an empty adapter.
        adapter = CurrencyAdapter(this, mutableListOf(), emptyMap())
        binding.currencyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.currencyRecyclerView.adapter = adapter

        // (Optional) Set up other UI elements (drawer, update, calculator…)
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }
        binding.btnUpdate.setOnClickListener {
            // You can implement a refresh here if needed.
        }
        binding.btnCalculator.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }
        binding.tvLastUpdate.text = "Updated 11:31 14.08.2025"

        // When the user presses "Add", show the bottom sheet.
        binding.btnAdd.setOnClickListener {
            showCurrencyBottomSheet()
        }

        // Launch a coroutine to fetch the full list of currencies and also the EUR rates.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the full list of currencies from the API.
                val currenciesMap = api.getAllCurrencies() // Map<String, String>
                // Convert the map to a list of CurrencyInfo.
                fullCurrencyList = currenciesMap.map { CurrencyInfo(it.key, it.value) }
                // Also fetch the EUR-based rates.
                val eurResponse: EurResponse = api.getEurRates()
                val ratesMap = eurResponse.eur // Map<String, Double>
                withContext(Dispatchers.Main) {
                    // Update our adapter’s exchangeRates map.
                    adapter.exchangeRates = ratesMap
                    // (Since the main list is initially empty, nothing to update there.)
                    Log.d("MainActivity", "Fetched ${fullCurrencyList.size} currencies and rates: $ratesMap")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching currencies or rates: ${e.message}")
            }
        }
    }

    // Show bottom sheet for adding a currency.
    private fun showCurrencyBottomSheet() {
        // Inflate the bottom sheet layout. We pass the activity's content as the parent.
        val parent = findViewById<ViewGroup>(android.R.id.content)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_currencies, parent, false)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        // Find views in the bottom sheet.
        val btnClose = bottomSheetView.findViewById<ImageView>(R.id.btnClose)
        val btnSearch = bottomSheetView.findViewById<ImageView>(R.id.btnSearch)
        val tvTitle = bottomSheetView.findViewById<TextView>(R.id.tvTitle)
        val etSearch = bottomSheetView.findViewById<EditText>(R.id.etSearch)
        // In this version, the bottom sheet layout contains a RecyclerView (id: currenciesRecycler).
        val recycler = bottomSheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.currenciesRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        // Set the adapter for the bottom sheet.
        recycler.adapter = CurrenciesBottomSheetAdapter(fullCurrencyList) { pickedCode ->
            // When the user selects a currency, fetch its rate.
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val eurResponse: EurResponse = api.getEurRates()
                    val rate = eurResponse.eur[pickedCode] ?: 0.0
                    withContext(Dispatchers.Main) {
                        // Add a new row with the selected currency code and its rate.
                        adapter.addNewCurrency(pickedCode, rate)
                        bottomSheetDialog.dismiss()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error fetching rate: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Set up search functionality.
        btnClose.setOnClickListener { bottomSheetDialog.dismiss() }
        btnSearch.setOnClickListener {
            if (etSearch.visibility == View.GONE) {
                btnSearch.setImageResource(R.drawable.baseline_close_24)
                tvTitle.visibility = View.GONE
                etSearch.visibility = View.VISIBLE
                etSearch.post { etSearch.requestFocus() }
            } else {
                btnSearch.setImageResource(R.drawable.baseline_search_24)
                tvTitle.visibility = View.VISIBLE
                etSearch.visibility = View.GONE
                etSearch.setText("")
                (recycler.adapter as? CurrenciesBottomSheetAdapter)?.updateData(fullCurrencyList)
            }
        }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = fullCurrencyList.filter {
                    it.code.lowercase().contains(query) || it.name.lowercase().contains(query)
                }
                (recycler.adapter as? CurrenciesBottomSheetAdapter)?.updateData(filtered)
            }
        })

        bottomSheetDialog.show()
    }
}
