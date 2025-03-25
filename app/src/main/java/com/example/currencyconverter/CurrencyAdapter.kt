package com.example.currencyconverter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.example.currencyconverter.databinding.ItemCurrencyRowBinding
import com.example.domain.CurrencyInfo
import com.google.android.material.bottomsheet.BottomSheetDialog

class CurrencyAdapter(
    private val context: Context,
    private val items: MutableList<CurrencyItem>,
    var exchangeRates: Map<String, Double>
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    // We'll have the API currency list passed from MainActivity.
    var currencyList: List<CurrencyInfo> = listOf()

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

    override fun getItemCount() = items.size

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

    inner class CurrencyViewHolder(val binding: ItemCurrencyRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

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
            currentTextWatcher?.let { binding.etValue.removeTextChangedListener(it) }
            binding.etValue.setText(item.value.toString())
            currentTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!binding.etValue.hasFocus() || isUpdating) return
                    val text = s.toString()
                    if (text.isEmpty()) {
                        isUpdating = true
                        binding.etValue.removeTextChangedListener(this)
                        binding.etValue.setText("0")
                        binding.etValue.setSelection(binding.etValue.text.length)
                        binding.etValue.addTextChangedListener(this)
                        val currentRate = exchangeRates[item.currency] ?: 1.0
                        val base = 0.0 / currentRate
                        items.forEachIndexed { index, _ ->
                            val targetRate = exchangeRates[items[index].currency] ?: 1.0
                            items[index].value = base * targetRate
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
                        return
                    }
                    val enteredValue = text.toDoubleOrNull() ?: return
                    val currentRate = exchangeRates[item.currency] ?: 1.0
                    val base = enteredValue / currentRate
                    isUpdating = true
                    items.forEachIndexed { index, _ ->
                        val targetRate = exchangeRates[items[index].currency] ?: 1.0
                        items[index].value = base * targetRate
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
        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_currencies, null)
        val bottomSheetDialog = BottomSheetDialog(context)
        bottomSheetDialog.setContentView(bottomSheetView)
        val btnClose = bottomSheetView.findViewById<ImageView>(R.id.btnClose)
        val btnSearch = bottomSheetView.findViewById<ImageView>(R.id.btnSearch)
        val tvTitle = bottomSheetView.findViewById<TextView>(R.id.tvTitle)
        val etSearch = bottomSheetView.findViewById<EditText>(R.id.etSearch)
        val container = bottomSheetView.findViewById<ConstraintLayout>(R.id.currencyListContainer)

        // Use the currency list provided to the adapter.
        val currencyData = currencyList

        populateCurrencyList(currencyData, container) { chosen ->
            items[position] = items[position].copy(currency = chosen)
            notifyItemChanged(position)
            bottomSheetDialog.dismiss()
        }

        btnClose.setOnClickListener { bottomSheetDialog.dismiss() }

        var isSearching = false
        btnSearch.setOnClickListener {
            isSearching = !isSearching
            if (isSearching) {
                btnSearch.setImageResource(R.drawable.baseline_close_24)
                tvTitle.visibility = View.GONE
                etSearch.visibility = View.VISIBLE
                etSearch.post { etSearch.requestFocus() }
            } else {
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

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = currencyData.filter {
                    it.code.lowercase().contains(query) || it.name.lowercase().contains(query)
                }
                populateCurrencyList(filtered, container) { chosen ->
                    items[position] = items[position].copy(currency = chosen)
                    notifyItemChanged(position)
                    bottomSheetDialog.dismiss()
                }
            }
        })

        bottomSheetDialog.show()
    }

    private fun populateCurrencyList(
        data: List<CurrencyInfo>,
        container: ConstraintLayout,
        onCurrencySelected: (String) -> Unit
    ) {
        container.removeAllViews()
        var previousId = View.NO_ID

        data.forEach { info ->
            val rowId = View.generateViewId()
            val rowView = LayoutInflater.from(context).inflate(R.layout.item_currency_info, container, false)
            rowView.id = rowId

            val tvCode = rowView.findViewById<TextView>(R.id.tvCode)
            // Use "name" instead of "country" because our domain model has "name"
            val tvName = rowView.findViewById<TextView>(R.id.tvCountry)
            tvCode.text = info.code
            tvName.text = info.name

            rowView.setOnClickListener {
                onCurrencySelected(info.code)
            }

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
