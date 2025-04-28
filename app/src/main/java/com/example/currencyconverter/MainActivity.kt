package com.example.currencyconverter

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CurrencyAdapter
    private var fullCurrencyList: List<CurrencyInfo> = emptyList()
    private var selectedBasePosition = 0

    // Retrofit API
    private val api: CurrencyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApi::class.java)
    }

    // prefs key for saved currency codes + theme
    private val PREFS = "settings"
    private val KEY_CURRENCY_LIST = "currency_list"
    private val KEY_THEME = "theme_mode"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 0) restore theme
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(
            prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Setup nav‐drawer items
        val nav = findViewById<NavigationView>(R.id.navViewContainer)
        nav.findViewById<SwitchMaterial>(R.id.switchDarkMode).apply {
            isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            setOnCheckedChangeListener { _, checked ->
                val mode =
                    if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                AppCompatDelegate.setDefaultNightMode(mode)
                prefs.edit().putInt(KEY_THEME, mode).apply()
            }
        }
        nav.findViewById<View>(R.id.infoRow).setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        listOf(
            R.id.languageRow to "Language clicked",
            R.id.shareRow to "Share clicked",
            R.id.feedbackRow to "Feedback clicked",
            R.id.rateRow to "Rate clicked"
        ).forEach { (id, msg) ->
            nav.findViewById<View>(id).setOnClickListener {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // 2) Recycler + Adapter
        adapter = CurrencyAdapter(
            context = this,
            items = mutableListOf(),
            exchangeRates = emptyMap(),
            onCurrencyChangeRequested = { pos ->
                selectedBasePosition = pos
                showCurrencyBottomSheet { code -> changeCurrencyForItem(pos, code) }
            },
            onValueChanged = { basePos ->
                selectedBasePosition = basePos
                adapter.recalculateAll(basePos)
            }
        )
        binding.currencyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        // 3) Drag & swipe
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = adapter.moveItem(vh.adapterPosition, target.adapterPosition).let {
                saveCurrencyList()
                true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                adapter.removeItem(vh.adapterPosition)
                saveCurrencyList()
                updateEmptyState()
            }
        }).attachToRecyclerView(binding.currencyRecyclerView)

        // 4) Buttons
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.btnAdd.setOnClickListener { showCurrencyBottomSheet(::addNewCurrencyRow) }
        binding.btnCalculator.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }

        // 5) Update + Retry
        val spinAnim = AnimationUtils.loadAnimation(this, R.anim.spin)
        binding.btnUpdate.setOnClickListener { loadData(spinAnim) }
        binding.btnScrollRetry.setOnClickListener { loadData(spinAnim) }

        // 6) initial load
        loadData(spinAnim)
    }

    private fun loadData(spinAnim: Animation) {
        // show spinner
        binding.ivSpinnerOverlay.apply {
            visibility = View.VISIBLE
            startAnimation(spinAnim)
        }
        binding.scrollErrorLayout.visibility = View.GONE
        binding.currencyRecyclerView.visibility = View.GONE
        binding.tvEmptyList.visibility = View.GONE

        if (!isNetworkAvailable()) {
            binding.ivSpinnerOverlay.clearAnimation()
            binding.ivSpinnerOverlay.visibility = View.GONE
            binding.scrollErrorLayout.visibility = View.VISIBLE
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allCurrencies = api.getAllCurrencies()
                val eurRates = api.getEurRates().eur

                withContext(Dispatchers.Main) {
                    fullCurrencyList = allCurrencies.map { CurrencyInfo(it.key, it.value) }
                    adapter.exchangeRates = eurRates

                    // restore or default
                    val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
                    val saved = prefs.getString(KEY_CURRENCY_LIST, null)
                    val newList = mutableListOf<CurrencyItem>()
                    when {
                        saved == null -> { // first run
                            listOf("eur", "usd", "uah").forEach { code ->
                                fullCurrencyList.find { it.code == code }?.let {
                                    newList += CurrencyItem(code, it.name, eurRates[code] ?: 1.0)
                                }
                            }
                        }

                        saved.isEmpty() -> {
                            // user cleared → leave empty
                        }

                        else -> {
                            saved.split(",").forEach { code ->
                                fullCurrencyList.firstOrNull { it.code == code }?.let {
                                    newList += CurrencyItem(code, it.name, eurRates[code] ?: 1.0)
                                }
                            }
                        }
                    }

                    // swap in one go
                    adapter.items.apply {
                        clear()
                        addAll(newList)
                    }
                    adapter.notifyDataSetChanged()

                    // toggle empty message
                    updateEmptyState()

                    // recalc if any
                    if (adapter.items.isNotEmpty()) adapter.recalculateAll(0)

                    // timestamp
                    val now = SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault())
                        .format(Date())
                    binding.tvLastUpdate.text = "Updated $now"

                    binding.ivSpinnerOverlay.clearAnimation()
                    binding.ivSpinnerOverlay.visibility = View.GONE
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.ivSpinnerOverlay.clearAnimation()
                    binding.ivSpinnerOverlay.visibility = View.GONE
                    binding.scrollErrorLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun addNewCurrencyRow(code: String) {
        // If list was empty, clear placeholder
        if (adapter.items.isEmpty()) {
            fullCurrencyList.firstOrNull { it.code == code }?.let {
                val rate = adapter.exchangeRates[code] ?: 1.0
                adapter.addNewCurrency(code, it.name, rate)
                selectedBasePosition = 0
                saveCurrencyList()
                updateEmptyState()
            }
            return
        }

        val base = adapter.items.getOrNull(selectedBasePosition) ?: return
        val baseE = base.value / (adapter.exchangeRates[base.currency] ?: 1.0)
        val rate = adapter.exchangeRates[code] ?: 1.0
        val name = fullCurrencyList.firstOrNull { it.code == code }?.name ?: code

        adapter.addNewCurrency(code, name, baseE * rate)
        selectedBasePosition = adapter.items.size - 1
        saveCurrencyList()
        updateEmptyState()
    }

    private fun changeCurrencyForItem(pos: Int, newCode: String) {
        val old = adapter.items[pos]
        val oldEur = old.value / (adapter.exchangeRates[old.currency] ?: 1.0)
        val newRate = adapter.exchangeRates[newCode] ?: 1.0
        val name = fullCurrencyList.firstOrNull { it.code == newCode }?.name ?: newCode

        adapter.updateItemCurrency(pos, newCode, name, oldEur * newRate)
        adapter.recalculateAll(pos)
        saveCurrencyList()
    }

    private fun saveCurrencyList() {
        val csv = adapter.items.joinToString(",") { it.currency }
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString(KEY_CURRENCY_LIST, csv)
            .apply()
    }

    private fun updateEmptyState() {
        if (adapter.items.isEmpty()) {
            binding.tvEmptyList.visibility = View.VISIBLE
            binding.currencyRecyclerView.visibility = View.GONE
        } else {
            binding.tvEmptyList.visibility = View.GONE
            binding.currencyRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showCurrencyBottomSheet(onPick: (String) -> Unit) {
        val parent = findViewById<ViewGroup>(android.R.id.content)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_currencies, parent, false)
        val dlg = BottomSheetDialog(this).apply { setContentView(view) }

        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val btnSearch = view.findViewById<ImageView>(R.id.btnSearch)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rv = view.findViewById<RecyclerView>(R.id.currenciesRecycler)

        rv.layoutManager = LinearLayoutManager(this)
        val sheetAdapter = CurrenciesBottomSheetAdapter(fullCurrencyList) {
            onPick(it)
            dlg.dismiss()
        }
        rv.adapter = sheetAdapter

        btnClose.setOnClickListener { dlg.dismiss() }
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
                etSearch.text.clear()
                sheetAdapter.updateData(fullCurrencyList)
            }
        }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, bf: Int, ac: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, ac: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().lowercase()
                sheetAdapter.updateData(
                    fullCurrencyList.filter {
                        it.code.lowercase().contains(q) ||
                                it.name.lowercase().contains(q)
                    }
                )
            }
        })
        dlg.show()
    }
}
