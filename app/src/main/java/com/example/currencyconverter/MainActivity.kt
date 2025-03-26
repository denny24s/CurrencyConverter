package com.example.currencyconverter

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.currencyconverter.databinding.ActivityMainBinding
import com.example.domain.CurrencyInfo
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Obtain ViewModel via Hilt.
    private val viewModel: MainViewModel by viewModels()

    // This list will hold the full currency list from API (domain model).
    private var apiCurrencyList: List<CurrencyInfo> = listOf()

    // Our adapter for the main RecyclerView.
    private lateinit var adapter: CurrencyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the RecyclerView with an empty adapter initially.
        adapter = CurrencyAdapter(this, mutableListOf(), emptyMap())
        binding.currencyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.currencyRecyclerView.adapter = adapter

        // Observe exchange rates LiveData.
        viewModel.exchangeRatesLiveData.observe(this) { rates ->
            Log.d("MainActivity", "Exchange rates received: ${rates.rates}")
            // Update the adapter’s exchangeRates map.
            adapter.exchangeRates = rates.rates
        }

        // Observe the full currency list LiveData.
        viewModel.currencyListLiveData.observe(this) { list ->
            Log.d("MainActivity", "Currency list received: $list")
            apiCurrencyList = list
            adapter.currencyList = apiCurrencyList
        }

        // Open navigation drawer.
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }

        // Update button – trigger refresh if needed.
        binding.btnUpdate.setOnClickListener {
            // TODO: Trigger refresh logic in your ViewModel.
        }
        binding.tvLastUpdate.text = "Updated 11:31 14.08.2025"

        // When the user presses "Add", show the bottom sheet.
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
        // Inflate the bottom sheet with a non-null parent.
        val parent = findViewById<ViewGroup>(android.R.id.content)
        val bottomSheetView = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_currencies, parent, false)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        val btnClose = bottomSheetView.findViewById<ImageView>(R.id.btnClose)
        val btnSearch = bottomSheetView.findViewById<ImageView>(R.id.btnSearch)
        val tvTitle = bottomSheetView.findViewById<TextView>(R.id.tvTitle)
        val etSearch = bottomSheetView.findViewById<EditText>(R.id.etSearch)
        // Use a RecyclerView inside the bottom sheet.
        val recycler = bottomSheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.currenciesRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        // Create an inline adapter for the bottom sheet.
        val items: List<CurrencyInfo> = apiCurrencyList
        recycler.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<CurrencyBottomSheetVH>(),
            CurrencyBottomSheetAdapterUpdater {
            private var data: List<CurrencyInfo> = items
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyBottomSheetVH {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_currency_info, parent, false)
                return CurrencyBottomSheetVH(view)
            }
            override fun onBindViewHolder(holder: CurrencyBottomSheetVH, position: Int) {
                val info = data[position]
                holder.tvCode.text = info.code
                holder.tvName.text = info.name
                holder.itemView.setOnClickListener {
                    adapter.addNewCurrency(info.code)
                    bottomSheetDialog.dismiss()
                }
            }
            override fun getItemCount(): Int = data.size
            override fun updateData(newData: List<CurrencyInfo>) {
                data = newData
                notifyDataSetChanged()
            }
        }

        // Set up search functionality.
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.lowercase() ?: ""
                val filtered = apiCurrencyList.filter { currency ->
                    currency.code.lowercase().contains(query) || currency.name.lowercase().contains(query)
                }
                (recycler.adapter as? CurrencyBottomSheetAdapterUpdater)?.updateData(filtered)
            }
        })

        btnClose.setOnClickListener { bottomSheetDialog.dismiss() }

        btnSearch.setOnClickListener {
            if (etSearch.visibility == android.view.View.GONE) {
                btnSearch.setImageResource(R.drawable.baseline_close_24)
                tvTitle.visibility = android.view.View.GONE
                etSearch.visibility = android.view.View.VISIBLE
                etSearch.post { etSearch.requestFocus() }
            } else {
                btnSearch.setImageResource(R.drawable.baseline_search_24)
                tvTitle.visibility = android.view.View.VISIBLE
                etSearch.visibility = android.view.View.GONE
                etSearch.setText("")
                (recycler.adapter as? CurrencyBottomSheetAdapterUpdater)?.updateData(apiCurrencyList)
            }
        }

        bottomSheetDialog.show()
    }
}

// ViewHolder for the bottom sheet RecyclerView.
class CurrencyBottomSheetVH(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
    val tvCode: TextView = view.findViewById(R.id.tvCode)
    val tvName: TextView = view.findViewById(R.id.tvCountry)
}

// Interface for updating the bottom sheet adapter.
interface CurrencyBottomSheetAdapterUpdater {
    fun updateData(newData: List<CurrencyInfo>)
}
