package com.example.currencyconverter.ui.main

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.currencyconverter.CurrencyItem
import com.example.currencyconverter.R
import com.example.currencyconverter.databinding.ItemCurrencyRowBinding

class CurrencyAdapter(
    val context: Context,
    val items: MutableList<CurrencyItem>,
    var exchangeRates: Map<String, Double>,
    private val onCurrencyChangeRequested: (position: Int) -> Unit,
    private val onValueChanged: (basePosition: Int) -> Unit
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val binding = ItemCurrencyRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CurrencyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun onBindViewHolder(
        holder: CurrencyViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_VALUE_ONLY)) {
            holder.updateValueOnly(items[position].value)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount(): Int = items.size

    fun addNewCurrency(code: String, name: String, rate: Double) {
        items.add(CurrencyItem(currency = code, currencyName = name, value = rate))
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

    fun updateItemCurrency(position: Int, newCode: String, newName: String, newValue: Double) {
        val item = items[position]
        item.currency = newCode
        item.currencyName = newName
        item.value = newValue
        notifyItemChanged(position)
    }

    private companion object {
        const val PAYLOAD_VALUE_ONLY = "value"
    }


    fun recalculateAll(basePosition: Int) {
        if (basePosition !in items.indices) return

        val baseItem = items[basePosition]
        val baseRate = exchangeRates[baseItem.currency] ?: 1.0
        val baseValueInEur = baseItem.value / baseRate

        for (i in items.indices) {
            if (i == basePosition) continue
            val rate = exchangeRates[items[i].currency] ?: 1.0
            items[i].value = baseValueInEur * rate
            notifyItemChanged(i, PAYLOAD_VALUE_ONLY)
        }
    }

    private fun formatValue(v: Double): String =
        String.format("%.2f", v)


    inner class CurrencyViewHolder(val binding: ItemCurrencyRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentTextWatcher: TextWatcher? = null

        fun updateValueOnly(newVal: Double) {
            val et = binding.etValue
            val sel = et.selectionStart
            et.setText(String.format("%.2f", newVal))
            val newPos = sel.coerceAtMost(et.text.length)
            et.setSelection(newPos)
        }

        fun bind(item: CurrencyItem, position: Int) {

            binding.tvCurrency.text = item.currency.uppercase()
            binding.tvCurrencyName.text = item.currencyName

            val accent = binding.vAccent
            val rowRoot = binding.root

            fun updateFocusState(hasFocus: Boolean) {
                accent.visibility = if (hasFocus) View.VISIBLE else View.GONE
                rowRoot.setBackgroundColor(
                    if (hasFocus)
                        ContextCompat.getColor(context, R.color.row_gray)
                    else
                        Color.TRANSPARENT
                )
            }

            updateFocusState(binding.etValue.hasFocus())

            binding.etValue.setOnFocusChangeListener { _, hasFocus ->
                updateFocusState(hasFocus)
            }


            binding.currencyChangeContainer.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                onCurrencyChangeRequested(pos)
            }

            currentTextWatcher?.let { binding.etValue.removeTextChangedListener(it) }

            binding.etValue.setText(formatValue(item.value))
            binding.etValue.setSelection(binding.etValue.text.length)

            binding.etValue.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable?) {
                        if (!binding.etValue.hasFocus()) return

                        val text = s?.toString().orEmpty()
                        if (text.isEmpty()) {
                            item.value = 0.0
                            binding.etValue.removeTextChangedListener(this)
                            binding.etValue.setText("0")
                            binding.etValue.setSelection(0, 1)
                            binding.etValue.addTextChangedListener(this)
                            onValueChanged(position)
                            return
                        }

                        item.value = text.toDoubleOrNull() ?: 0.0
                        onValueChanged(position)
                    }
                }.also { currentTextWatcher = it }
            )
        }
    }
}

