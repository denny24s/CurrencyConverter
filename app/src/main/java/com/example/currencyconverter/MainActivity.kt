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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.currencyconverter.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CurrencyAdapter


    // We'll track which row is the "base" (the last row user typed on or changed currency).
    private var selectedBasePosition: Int = 0

    private val api: CurrencyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApi::class.java)
    }

    private var fullCurrencyList: List<CurrencyInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 0) read saved mode
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val savedMode = prefs.getInt(
            "theme_mode",
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        AppCompatDelegate.setDefaultNightMode(savedMode)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3) now it’s safe to reference binding.* or call findViewById
        val navViewContainer = findViewById<NavigationView>(R.id.navViewContainer)
        val switchDark = navViewContainer.findViewById<SwitchMaterial>(R.id.switchDarkMode)

        switchDark.isChecked =
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        // 4) Wire up the toggle
        switchDark.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO

            AppCompatDelegate.setDefaultNightMode(mode)
            //  save
            prefs.edit()
                .putInt("theme_mode", mode)
                .apply()
        }


        adapter = CurrencyAdapter(
            context = this,
            items = mutableListOf(),
            exchangeRates = emptyMap(),
            onCurrencyChangeRequested = { position ->
                // Mark that row as the base
                selectedBasePosition = position
                // Show bottom sheet
                showCurrencyBottomSheet { pickedCode ->
                    changeCurrencyForItem(position, pickedCode)
                }
            },
            onValueChanged = { basePosition ->
                selectedBasePosition = basePosition
                adapter.recalculateAll(basePosition)
            }
        )

        binding.currencyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.currencyRecyclerView.adapter = adapter

        // Enable drag & swipe
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, // drag
            ItemTouchHelper.LEFT // swipe left
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                adapter.moveItem(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                adapter.removeItem(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.currencyRecyclerView)

        // Drawer, update, calculator
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }
        binding.btnUpdate.setOnClickListener {
            // refresh logic if needed
        }
        binding.btnCalculator.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }
        binding.tvLastUpdate.text = "Updated 11:31 14.08.2025"

        // Add new currency
        binding.btnAdd.setOnClickListener {
            // We'll pick the "base" row as the row that currently has focus or was last updated
            showCurrencyBottomSheet { pickedCode ->
                addNewCurrencyRow(pickedCode)
            }
        }

        // Fetch currencies + EUR-based rates
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currenciesMap = api.getAllCurrencies()
                fullCurrencyList = currenciesMap.map { CurrencyInfo(it.key, it.value) }
                val eurResponse = api.getEurRates()
                val ratesMap = eurResponse.eur

                withContext(Dispatchers.Main) {
                    adapter.exchangeRates = ratesMap
                    // 1) Add 3 default rows: EUR, USD, UAH (with correct initial values)
                    initDefaultCurrencies()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching: ${e.message}")
            }
        }
    }

    // This function will add EUR, USD, UAH with correct math
    private fun initDefaultCurrencies() {
        // Suppose EUR=1.0 as the baseline
        val euroCode = "eur"
        val usdCode = "usd"
        val uahCode = "uah"

        // We definitely need these to be present in the rates map
        // But if not, let's default them to 1.0 just in case
        val rateEUR = adapter.exchangeRates[euroCode] ?: 1.0 // Usually 1.0
        val rateUSD = adapter.exchangeRates[usdCode] ?: 1.0
        val rateUAH = adapter.exchangeRates[uahCode] ?: 1.0

        // We'll add them in a standard order: EUR first, then USD, then UAH.
        // EUR = 1.0
        val nameEur = fullCurrencyList.find { it.code == "eur" }?.name ?: "Euro"
        adapter.addNewCurrency(euroCode, nameEur, rateEUR)
        // The newly added row is the last row, so let's recalc from the new row?
        // But we want it to stay as is for now. We'll do the final recalc after all are added.

        // USD = (1.0 * rateUSD / rateEUR) *but typically rateEUR is 1
        // Actually we want to say: If 1 EUR = <some> USD, then let's set the row's value to that
        // which is the ratio rateUSD / rateEUR
        val usdValue = (1.0 / rateEUR) * rateUSD
        val nameUsd = fullCurrencyList.find { it.code == "usd" }?.name ?: "US Dollar"
        adapter.addNewCurrency(usdCode, nameUsd, usdValue)

        // UAH = (1.0 * rateUAH / rateEUR)
        val uahValue = (1.0 / rateEUR) * rateUAH
        val nameUah = fullCurrencyList.find { it.code == "uah" }?.name ?: "Ukrainian Hryvnia"
        adapter.addNewCurrency(uahCode, nameUah, uahValue)

        // We'll recalc from the EUR row (which is position 0 after we inserted them).
        // That ensures everything is consistent.
        adapter.recalculateAll(0)
    }

    private fun addNewCurrencyRow(pickedCode: String) {
        // We'll do math based on the "selectedBasePosition".
        val baseItem = adapter.items.getOrNull(selectedBasePosition) ?: return
        val baseRate = adapter.exchangeRates[baseItem.currency] ?: 1.0
        val baseValueInEur = baseItem.value / baseRate

        val newRate = adapter.exchangeRates[pickedCode] ?: 1.0
        val newValue = baseValueInEur * newRate

        val name = fullCurrencyList.find { it.code == pickedCode }?.name ?: pickedCode
        adapter.addNewCurrency(
            code = pickedCode,
            name = name,
            rate = newValue
        )
        // The newly added row is last in the list
        val newRowPos = adapter.items.size - 1
        adapter.notifyItemChanged(newRowPos)
    }

    private fun changeCurrencyForItem(position: Int, newCode: String) {
        // We'll do math based on that same position
        val baseItem = adapter.items[position]
        val oldRate = adapter.exchangeRates[baseItem.currency] ?: 1.0
        val oldValueInEur = baseItem.value / oldRate

        val newRate = adapter.exchangeRates[newCode] ?: 1.0
        val newValue = oldValueInEur * newRate

        val name = fullCurrencyList.find { it.code == newCode }?.name ?: newCode
        adapter.updateItemCurrency(position, newCode, name, newValue)
        // Recalculate from this row
        adapter.recalculateAll(position)
    }

    private fun showCurrencyBottomSheet(onPick: (String) -> Unit) {
        val parent = findViewById<ViewGroup>(android.R.id.content)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_currencies, parent, false)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        val btnClose = bottomSheetView.findViewById<ImageView>(R.id.btnClose)
        val btnSearch = bottomSheetView.findViewById<ImageView>(R.id.btnSearch)
        val tvTitle = bottomSheetView.findViewById<TextView>(R.id.tvTitle)
        val etSearch = bottomSheetView.findViewById<EditText>(R.id.etSearch)
        val recycler = bottomSheetView.findViewById<RecyclerView>(R.id.currenciesRecycler)

        recycler.layoutManager = LinearLayoutManager(this)
        val sheetAdapter = CurrenciesBottomSheetAdapter(fullCurrencyList) { pickedCode ->
            CoroutineScope(Dispatchers.Main).launch {
                onPick(pickedCode)
                bottomSheetDialog.dismiss()
            }
        }
        recycler.adapter = sheetAdapter

        btnClose.setOnClickListener { bottomSheetDialog.dismiss() }
        btnSearch.setOnClickListener {
            if (etSearch.visibility == View.GONE) {
                btnSearch.setImageResource(R.drawable.baseline_close_24)
                tvTitle.visibility = View.GONE
                etSearch.visibility = View.VISIBLE
                etSearch.requestFocus()
            } else {
                btnSearch.setImageResource(R.drawable.baseline_search_24)
                tvTitle.visibility = View.VISIBLE
                etSearch.visibility = View.GONE
                etSearch.setText("")
                sheetAdapter.updateData(fullCurrencyList)
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
                sheetAdapter.updateData(filtered)
            }
        })

        bottomSheetDialog.show()
    }
}
