package com.example.currencyconverter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.currencyconverter.databinding.ItemCurrencyRowBinding


class CurrencyAdapter(
    private val context: Context,
    private val items: MutableList<CurrencyItem>,
    // Exchange rates map retrieved from the API (with base EUR)
    var exchangeRates: Map<String, Double>
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    // This list will be set from MainActivity (the full list of currencies).
    var currencyList: List<com.example.domain.CurrencyInfo> = listOf()

    private var isUpdating = false
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

    override fun getItemCount(): Int = items.size

    // Add a new currency row. Its value is simply retrieved from the exchangeRates map.
    fun addNewCurrency(chosen: String) {
        val newValue = exchangeRates[chosen] ?: 0.0
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

    inner class CurrencyViewHolder(val binding: ItemCurrencyRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentTextWatcher: TextWatcher? = null

        fun bind(item: CurrencyItem) {
            binding.tvCurrency.text = item.currency
            currentTextWatcher?.let { binding.etValue.removeTextChangedListener(it) }
            // Set the value (retrieved from the API exchange rates).
            binding.etValue.setText(item.value.toString())
            // Allow the user to edit the value if desired.
            currentTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!binding.etValue.hasFocus() || isUpdating) return
                    item.value = s.toString().toDoubleOrNull() ?: 0.0
                }
            }
            binding.etValue.addTextChangedListener(currentTextWatcher)
        }
    }
}
