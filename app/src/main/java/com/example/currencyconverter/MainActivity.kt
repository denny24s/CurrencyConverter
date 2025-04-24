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
import android.widget.FrameLayout
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CurrencyAdapter
    private var fullCurrencyList: List<CurrencyInfo> = emptyList()
    private var selectedBasePosition = 0

    private val api: CurrencyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        // 0) Restore saved theme
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val savedMode = prefs.getInt(
            "theme_mode",
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        AppCompatDelegate.setDefaultNightMode(savedMode)

        // 1) Inflate + setContentView
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2) Dark‑mode switch in your nav drawer
        val nav = findViewById<NavigationView>(R.id.navViewContainer)
        val sw = nav.findViewById<SwitchMaterial>(R.id.switchDarkMode)
        sw.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        sw.setOnCheckedChangeListener { _, checked ->
            val mode = if (checked) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
            prefs.edit().putInt("theme_mode", mode).apply()
        }

        // Info:
        nav.findViewById<View>(R.id.infoRow).setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

// The four “toast” rows:
        listOf(
            R.id.languageRow    to "Language clicked",
            R.id.shareRow       to "Share clicked",
            R.id.feedbackRow    to "Feedback clicked",
            R.id.rateRow        to "Rate clicked"
        ).forEach { (id, msg) ->
            nav.findViewById<View>(id).setOnClickListener {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
        // 3) Setup RecyclerView + Adapter
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
        binding.currencyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.currencyRecyclerView.adapter = adapter

        // 4) Drag & Swipe
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = adapter.moveItem(vh.adapterPosition, target.adapterPosition).let { true }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                adapter.removeItem(vh.adapterPosition)
            }
        }).attachToRecyclerView(binding.currencyRecyclerView)

        // 5) Drawer + other buttons
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }
        binding.btnUpdate.setOnClickListener {
            // you can call loadData() here if you want refresh on update press
        }
        binding.btnCalculator.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }
        binding.btnAdd.setOnClickListener {
            showCurrencyBottomSheet(::addNewCurrencyRow)
        }

        // 6) Spinner animation + retry
        val spinAnim: Animation = AnimationUtils.loadAnimation(this, R.anim.spin)
        binding.btnScrollRetry.setOnClickListener { loadData(spinAnim) }

        // 7) Initial load
        loadData(spinAnim)
    }

    private fun loadData(spinAnim: Animation) {
        // 1) hide list + error, show spinner
        binding.currencyRecyclerView.visibility = View.GONE
        binding.scrollErrorLayout.visibility   = View.GONE
        binding.ivSpinnerOverlay.apply {
            visibility = View.VISIBLE
            startAnimation(spinAnim)
        }

        // 2) offline?
        if (!isNetworkAvailable()) {
            binding.ivSpinnerOverlay.apply {
                clearAnimation()
                visibility = View.GONE
            }
            binding.scrollErrorLayout.visibility = View.VISIBLE
            binding.btnScrollRetry.setOnClickListener { loadData(spinAnim) }
            return
        }

        // 3) fetch
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val all = api.getAllCurrencies()
                val rates = api.getEurRates().eur
                withContext(Dispatchers.Main) {
                    fullCurrencyList = all.map { CurrencyInfo(it.key, it.value) }
                    adapter.exchangeRates = rates
                    initDefaultCurrencies()
                    // show list
                    binding.ivSpinnerOverlay.apply {
                        clearAnimation()
                        visibility = View.GONE
                    }
                    binding.currencyRecyclerView.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    binding.ivSpinnerOverlay.apply {
                        clearAnimation()
                        visibility = View.GONE
                    }
                    binding.scrollErrorLayout.visibility = View.VISIBLE
                    binding.btnScrollRetry.setOnClickListener { loadData(spinAnim) }
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun initDefaultCurrencies() {
        val euro = "eur"; val usd = "usd"; val uah = "uah"
        val r = adapter.exchangeRates
        val eRate = r[euro] ?: 1.0
        val uRate = r[usd]  ?: 1.0
        val hRate = r[uah]  ?: 1.0

        fullCurrencyList.find { it.code==euro }?.let {
            adapter.addNewCurrency(euro, it.name, eRate)
        }
        fullCurrencyList.find { it.code==usd }?.let {
            adapter.addNewCurrency(usd, it.name, (1.0/eRate)*uRate)
        }
        fullCurrencyList.find { it.code==uah }?.let {
            adapter.addNewCurrency(uah, it.name, (1.0/eRate)*hRate)
        }

        adapter.recalculateAll(0)
    }

    private fun addNewCurrencyRow(code: String) {
        val base = adapter.items.getOrNull(selectedBasePosition) ?: return
        val baseValEur = base.value / (adapter.exchangeRates[base.currency] ?: 1.0)
        val newRate = adapter.exchangeRates[code] ?: 1.0
        val name = fullCurrencyList.find { it.code==code }?.name ?: code
        adapter.addNewCurrency(code, name, baseValEur * newRate)
    }

    private fun changeCurrencyForItem(pos: Int, newCode: String) {
        val old = adapter.items[pos]
        val oldValEur = old.value / (adapter.exchangeRates[old.currency] ?: 1.0)
        val newRate = adapter.exchangeRates[newCode] ?: 1.0
        val name = fullCurrencyList.find { it.code==newCode }?.name ?: newCode
        adapter.updateItemCurrency(pos, newCode, name, oldValEur * newRate)
        adapter.recalculateAll(pos)
    }

    private fun showCurrencyBottomSheet(onPick: (String)->Unit) {
        val parent = findViewById<ViewGroup>(android.R.id.content)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_currencies, parent, false)
        val dlg = BottomSheetDialog(this).also { it.setContentView(view) }

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
