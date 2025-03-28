package com.example.currencyconverter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.currencyconverter.databinding.ItemCurrencyBinding

// Adapter updater interface to allow search filtering.
interface CurrenciesBottomSheetAdapterUpdater {
    fun updateData(newData: List<CurrencyInfo>)
}

class CurrenciesBottomSheetAdapter(
    private var data: List<CurrencyInfo>,
    private val onCurrencySelected: (String) -> Unit
) : RecyclerView.Adapter<CurrenciesBottomSheetAdapter.MyVH>(), CurrenciesBottomSheetAdapterUpdater {

    class MyVH(val binding: ItemCurrencyBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvName: TextView = binding.currencyText
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyVH {
        val binding = ItemCurrencyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyVH(binding)
    }

    override fun onBindViewHolder(holder: MyVH, position: Int) {
        val info = data[position]
        holder.tvName.text = info.name  // Display full currency name
        holder.itemView.setOnClickListener {
            onCurrencySelected(info.code)
        }
    }

    override fun getItemCount(): Int = data.size

    override fun updateData(newData: List<CurrencyInfo>) {
        data = newData
        notifyDataSetChanged()
    }
}
