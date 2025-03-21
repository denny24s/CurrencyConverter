package com.example.currencyconverter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.currencyconverter.databinding.ItemCurrencyRowBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class CurrencyAdapter(
    private val context: Context,
    private val items: MutableList<CurrencyItem>
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    // Fixed exchange rates: USD=1, UAH=40, RUB=80.
    companion object {
        val exchangeRates = mapOf(
            "USD" to 1.0,
            "UAH" to 40.0,
            "RUB" to 80.0
        )
    }

    // Flag to prevent recursive updates.
    private var isUpdating = false

    // Reference to the attached RecyclerView.
    private var recyclerViewRef: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerViewRef = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val binding = ItemCurrencyRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CurrencyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    // Add new currency using current base conversion.
    fun addNewCurrency(chosen: String) {
        val newValue = if (items.isNotEmpty()) {
            val ref = items[0]
            val base = ref.value / (exchangeRates[ref.currency] ?: 1.0)
            base * (exchangeRates[chosen] ?: 1.0)
        } else {
            1.0
        }
        addItem(CurrencyItem(chosen, newValue))
    }

    fun addItem(item: CurrencyItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun moveItem(fromPos: Int, toPos: Int) {
        val fromItem = items.removeAt(fromPos)
        items.add(toPos, fromItem)
        notifyItemMoved(fromPos, toPos)
    }

    inner class CurrencyViewHolder(
        val binding: ItemCurrencyRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Keep a reference for removal when rebinding.
        private var currentTextWatcher: TextWatcher? = null

        init {
            binding.currencyChangeContainer.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    showChangeCurrencyBottomSheet(pos)
                }
            }
        }

        fun bind(item: CurrencyItem) {
            binding.tvCurrency.text = item.currency

            // Remove previous watcher.
            currentTextWatcher?.let { binding.etValue.removeTextChangedListener(it) }

            // Set current text without triggering watcher.
            binding.etValue.setText(item.value.toString())

            // Create a new TextWatcher.
            currentTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // Only update if this field has focus and we're not doing a bulk update.
                    if (!binding.etValue.hasFocus() || isUpdating) return
                    val text = s.toString()
                    if (text.isEmpty()) {
                        isUpdating = true
                        // Remove watcher, set text to "0", and restore watcher.
                        binding.etValue.removeTextChangedListener(this)
                        binding.etValue.setText("0")
                        binding.etValue.setSelection(binding.etValue.text.length)
                        binding.etValue.addTextChangedListener(this)
                        // Update model: treat empty as 0.
                        val currentRate = exchangeRates[item.currency] ?: 1.0
                        val base = 0.0 / currentRate  // 0.0
                        for (i in items.indices) {
                            val targetRate = exchangeRates[items[i].currency] ?: 1.0
                            items[i].value = base * targetRate  // will be 0.0
                        }
                        // Update all visible holders (except the one being edited)
                        recyclerViewRef?.let { rv ->
                            for (i in 0 until itemCount) {
                                val holder = rv.findViewHolderForAdapterPosition(i) as? CurrencyViewHolder
                                if (holder != null && !holder.binding.etValue.hasFocus()) {
                                    holder.binding.etValue.setText(items[i].value.toString())
                                }
                            }
                        }
                        isUpdating = false
                        return
                    }
                    // Otherwise, process normally.
                    val enteredValue = text.toDoubleOrNull() ?: return
                    val currentRate = exchangeRates[item.currency] ?: 1.0
                    val base = enteredValue / currentRate

                    isUpdating = true
                    for (i in items.indices) {
                        val targetRate = exchangeRates[items[i].currency] ?: 1.0
                        items[i].value = base * targetRate
                    }
                    recyclerViewRef?.let { rv ->
                        for (i in 0 until itemCount) {
                            val holder = rv.findViewHolderForAdapterPosition(i) as? CurrencyViewHolder
                            if (holder != null && !holder.binding.etValue.hasFocus()) {
                                holder.binding.etValue.setText(items[i].value.toString())
                            }
                        }
                    }
                    isUpdating = false
                }


            }
            binding.etValue.addTextChangedListener(currentTextWatcher)
        }
    }

    private fun showChangeCurrencyBottomSheet(position: Int) {
        // 1) Inflate bottom sheet layout
        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_currencies, null)
        val bottomSheetDialog = BottomSheetDialog(context)
        bottomSheetDialog.setContentView(bottomSheetView)

        // 2) Find views
        val btnClose = bottomSheetView.findViewById<ImageView>(R.id.btnClose)
        val btnSearch = bottomSheetView.findViewById<ImageView>(R.id.btnSearch)
        val tvTitle  = bottomSheetView.findViewById<TextView>(R.id.tvTitle)
        val etSearch = bottomSheetView.findViewById<EditText>(R.id.etSearch)
        val container = bottomSheetView.findViewById<ConstraintLayout>(R.id.currencyListContainer)

        // 3) Sample data - or real data
        val currencyData = listOf(
            CurrencyInfo("USD", "United States"),
            CurrencyInfo("UAH", "Ukraine"),
            CurrencyInfo("RUB", "Russia"),
            // Add more if you want
        )

        // 4) Populate list
        populateCurrencyList(currencyData, container) { chosen ->
            // Update the row's currency
            items[position] = items[position].copy(currency = chosen)
            notifyItemChanged(position)
            bottomSheetDialog.dismiss()
        }

        // Close button
        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Search icon toggles
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
                populateCurrencyList(currencyData, container) { chosen ->
                    items[position] = items[position].copy(currency = chosen)
                    notifyItemChanged(position)
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
                populateCurrencyList(filtered, container) { chosen ->
                    items[position] = items[position].copy(currency = chosen)
                    notifyItemChanged(position)
                    bottomSheetDialog.dismiss()
                }
            }
        })

        // 5) Show bottom sheet
        bottomSheetDialog.show()
    }

    /** Dynamically inflates each currency row into the container. */
    private fun populateCurrencyList(
        data: List<CurrencyInfo>,
        container: ConstraintLayout,
        onCurrencySelected: (String) -> Unit
    ) {
        container.removeAllViews()
        var previousId = View.NO_ID

        data.forEach { info ->
            val rowId = View.generateViewId()
            val rowView = LayoutInflater.from(context)
                .inflate(R.layout.item_currency_info, container, false)
            rowView.id = rowId

            // Bind data
            val tvCode = rowView.findViewById<TextView>(R.id.tvCode)
            val tvCountry = rowView.findViewById<TextView>(R.id.tvCountry)
            tvCode.text = info.code
            tvCountry.text = info.country

            // On row click -> pick currency
            rowView.setOnClickListener {
                onCurrencySelected(info.code)
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
