package com.example.currencyconverter

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.currencyconverter.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CurrencyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView setup ...
        val initialItems = mutableListOf(
            CurrencyItem("USD", 1.0),
            CurrencyItem("UAH", 40.0),
            CurrencyItem("RUB", 80.0)
        )
        adapter = CurrencyAdapter(this, initialItems)
        binding.currencyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.currencyRecyclerView.adapter = adapter

        // Drag & Swipe
        val touchHelperCallback = CurrencyItemTouchHelperCallback(adapter)
        ItemTouchHelper(touchHelperCallback).attachToRecyclerView(binding.currencyRecyclerView)

        // Menu button -> open drawer
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Dark mode switch
        val switchDarkMode = findViewById<Switch>(R.id.switchDarkMode)
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Language row
        val languageRow = findViewById<android.view.View>(R.id.languageRow)
        languageRow.setOnClickListener {
            val languages = arrayOf("English", "English")
            AlertDialog.Builder(this)
                .setTitle("Choose language")
                .setItems(languages) { _, which ->
                    Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Share row
        findViewById<android.view.View>(R.id.shareRow).setOnClickListener {
            Toast.makeText(this, "Share clicked", Toast.LENGTH_SHORT).show()
        }

        // Info row
        findViewById<android.view.View>(R.id.infoRow).setOnClickListener {
            Toast.makeText(this, "Info clicked", Toast.LENGTH_SHORT).show()
        }

        // Feedback row
        findViewById<android.view.View>(R.id.feedbackRow).setOnClickListener {
            Toast.makeText(this, "Feedback clicked", Toast.LENGTH_SHORT).show()
        }

        // Rate row
        findViewById<android.view.View>(R.id.rateRow).setOnClickListener {
            Toast.makeText(this, "Rate clicked", Toast.LENGTH_SHORT).show()
        }

        // Version text
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        tvVersion.text = "Version 1.0.0"

        // Update button
        binding.btnUpdate.setOnClickListener {
            // TODO: fetch new rates
        }
        binding.tvLastUpdate.text = "Updated 11:31 14.08.2025"

        // Add currency row
        binding.btnAdd.setOnClickListener {
            showCurrencyBottomSheet()
        }

        // Calculator button
        binding.btnCalculator.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }
    }

    private fun showCurrencyPicker() {
        val currencies = arrayOf("USD", "UAH", "RUB")
        AlertDialog.Builder(this)
            .setTitle("Choose currency")
            .setItems(currencies) { _, which ->
                adapter.addNewCurrency(currencies[which])
            }
            .show()
    }

    private fun showCurrencyBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_currencies, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        val btnClose = bottomSheetView.findViewById<ImageView>(R.id.btnClose)
        val btnSearch = bottomSheetView.findViewById<ImageView>(R.id.btnSearch)
        val tvTitle  = bottomSheetView.findViewById<TextView>(R.id.tvTitle)
        val etSearch = bottomSheetView.findViewById<EditText>(R.id.etSearch)
        val container = bottomSheetView.findViewById<ConstraintLayout>(R.id.currencyListContainer)

        // Example data (add more if needed)
        val currencyData = listOf(
            CurrencyInfo("USD", "United States"),
            CurrencyInfo("UAH", "Ukraine"),
            CurrencyInfo("RUB", "Russia")
        )

        // Populate the bottom sheet list with a callback to add currency
        populateCurrencyList(currencyData, container) { chosenCode ->
            adapter.addNewCurrency(chosenCode)      // <--- add the currency row
            bottomSheetDialog.dismiss()
        }

        // Close icon
        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Toggle between search mode and normal mode
        var isSearching = false
        btnSearch.setOnClickListener {
            isSearching = !isSearching
            if (isSearching) {
                // Switch to search mode
                btnSearch.setImageResource(R.drawable.baseline_close_24)
                tvTitle.visibility = View.GONE
                etSearch.visibility = View.VISIBLE
                etSearch.requestFocus()
            } else {
                // Cancel search
                btnSearch.setImageResource(R.drawable.baseline_search_24)
                tvTitle.visibility = View.VISIBLE
                etSearch.visibility = View.GONE
                etSearch.setText("")
                // Reset list to all items
                populateCurrencyList(currencyData, container) { chosenCode ->
                    adapter.addNewCurrency(chosenCode)
                    bottomSheetDialog.dismiss()
                }
            }
        }

        // Filter as user types
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = currencyData.filter {
                    it.code.lowercase().contains(query) || it.country.lowercase().contains(query)
                }
                // Re-populate with filtered data
                populateCurrencyList(filtered, container) { chosenCode ->
                    adapter.addNewCurrency(chosenCode)
                    bottomSheetDialog.dismiss()
                }
            }
        })

        bottomSheetDialog.show()
    }


    /** Adds rows for each currency to the container. */
    private fun populateCurrencyList(
        data: List<CurrencyInfo>,
        container: ConstraintLayout,
        onCurrencySelected: (String) -> Unit
    ) {
        container.removeAllViews()
        var previousId = View.NO_ID

        data.forEach { info ->
            val rowId = View.generateViewId()
            val rowView = layoutInflater.inflate(R.layout.item_currency_info, container, false)
            rowView.id = rowId

            // Bind data
            val tvCode = rowView.findViewById<TextView>(R.id.tvCode)
            val tvCountry = rowView.findViewById<TextView>(R.id.tvCountry)
            tvCode.text = info.code
            tvCountry.text = info.country

            // On row click -> add new currency
            rowView.setOnClickListener {
                onCurrencySelected(info.code)  // <--- calls adapter.addNewCurrency(...)
            }

            container.addView(rowView)

            // Constrain each row below the previous one
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
