package com.example.currencyconverter

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.currencyconverter.databinding.ActivityMainBinding
import com.example.domain.CurrencyInfo
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // Currency list from API (domain model)
    private var apiCurrencyList: List<CurrencyInfo> = listOf()
    private lateinit var adapter: CurrencyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set a fallback adapter so RecyclerView always has one.
        adapter = CurrencyAdapter(this, mutableListOf(), emptyMap())
        binding.currencyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.currencyRecyclerView.adapter = adapter

        // Observe exchange rates LiveData (from API/Room cache)
        viewModel.exchangeRatesLiveData.observe(this) { rates ->
            // Create initial rows: Base is EUR (always 1.0) plus UAH and RUB from API.
            val initialItems = mutableListOf(
                CurrencyItem("EUR", 1.0),
                CurrencyItem("UAH", rates.rates["UAH"] ?: 0.0),
                CurrencyItem("RUB", rates.rates["RUB"] ?: 0.0)
            )
            // Create new adapter with the fetched exchange rates map.
            adapter = CurrencyAdapter(this, initialItems, rates.rates)
            // Set the API currency list (will be updated below).
            adapter.currencyList = apiCurrencyList
            binding.currencyRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.currencyRecyclerView.adapter = adapter

            // Attach drag & swipe functionality.
            val touchHelperCallback = CurrencyItemTouchHelperCallback(adapter)
            ItemTouchHelper(touchHelperCallback).attachToRecyclerView(binding.currencyRecyclerView)
        }

        // Observe currency list LiveData (from API/Room cache)
        viewModel.currencyListLiveData.observe(this) { list ->
            apiCurrencyList = list
            if (::adapter.isInitialized) {
                adapter.currencyList = apiCurrencyList
            }
        }

        // Open navigation drawer.
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Update button.
        binding.btnUpdate.setOnClickListener {
            // TODO: fetch new rates from API and update Room.
        }
        binding.tvLastUpdate.text = "Updated 11:31 14.08.2025"

        // Add currency row button.
        binding.btnAdd.setOnClickListener {
            showCurrencyBottomSheet()
        }

        // Calculator button.
        binding.btnCalculator.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }
    }

    // Show bottom sheet for adding a new currency.
    private fun showCurrencyBottomSheet() {
        // Get a valid parent (using the activity's content view) instead of passing null.
        val parent = findViewById<ViewGroup>(android.R.id.content)
        val bottomSheetView = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_currencies, parent, false)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        val btnClose = bottomSheetView.findViewById<ImageView>(R.id.btnClose)
        val btnSearch = bottomSheetView.findViewById<ImageView>(R.id.btnSearch)
        val tvTitle = bottomSheetView.findViewById<TextView>(R.id.tvTitle)
        val etSearch = bottomSheetView.findViewById<EditText>(R.id.etSearch)
        val container = bottomSheetView.findViewById<ConstraintLayout>(R.id.currencyListContainer)

        // Populate the list using the API currency list observed from ViewModel.
        populateCurrencyList(apiCurrencyList, container) { chosenCode ->
            adapter.addNewCurrency(chosenCode)
            bottomSheetDialog.dismiss()
        }

        btnClose.setOnClickListener { bottomSheetDialog.dismiss() }

        var isSearching = false
        btnSearch.setOnClickListener {
            isSearching = !isSearching
            if (isSearching) {
                btnSearch.setImageResource(R.drawable.baseline_close_24)
                tvTitle.visibility = android.view.View.GONE
                etSearch.visibility = android.view.View.VISIBLE
                etSearch.post { etSearch.requestFocus() }
            } else {
                btnSearch.setImageResource(R.drawable.baseline_search_24)
                tvTitle.visibility = android.view.View.VISIBLE
                etSearch.visibility = android.view.View.GONE
                etSearch.setText("")
                populateCurrencyList(apiCurrencyList, container) { chosenCode ->
                    adapter.addNewCurrency(chosenCode)
                    bottomSheetDialog.dismiss()
                }
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = apiCurrencyList.filter {
                    it.code.lowercase().contains(query) || it.name.lowercase().contains(query)
                }
                populateCurrencyList(filtered, container) { chosenCode ->
                    adapter.addNewCurrency(chosenCode)
                    bottomSheetDialog.dismiss()
                }
            }
        })

        bottomSheetDialog.show()
    }

    // Dynamically inflate each currency row into the provided container.
    private fun populateCurrencyList(
        data: List<CurrencyInfo>,
        container: ConstraintLayout,
        onCurrencySelected: (String) -> Unit
    ) {
        container.removeAllViews()
        var previousId = View.NO_ID
        data.forEach { info ->
            val rowId = View.generateViewId()
            val rowView = LayoutInflater.from(this)
                .inflate(R.layout.item_currency_info, container, false)
            rowView.id = rowId

            val tvCode = rowView.findViewById<TextView>(R.id.tvCode)
            // Our domain model uses "name" for the full currency name.
            val tvName = rowView.findViewById<TextView>(R.id.tvCountry)
            tvCode.text = info.code
            tvName.text = info.name

            rowView.setOnClickListener { onCurrencySelected(info.code) }
            container.addView(rowView)
            val set = ConstraintSet()
            set.clone(container)
            if (previousId == View.NO_ID) {
                set.connect(rowId, ConstraintSet.TOP, container.id, ConstraintSet.TOP, 16)
            } else {
                set.connect(rowId, ConstraintSet.TOP, previousId, ConstraintSet.BOTTOM, 16)
            }
            set.connect(rowId, ConstraintSet.START, container.id, ConstraintSet.START)
            set.connect(rowId, ConstraintSet.END, container.id, ConstraintSet.END)
            set.applyTo(container)
            previousId = rowId
        }
    }
}
