package com.example.currencyconverter.ui.main

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.currencyconverter.ui.calculator.CalculatorActivity
import com.example.currencyconverter.ui.add.CurrenciesBottomSheetAdapter
import com.example.currencyconverter.CurrencyItem
import com.example.currencyconverter.ui.info.InfoActivity
import com.example.currencyconverter.R
import com.example.currencyconverter.databinding.ActivityMainBinding
import com.example.domain.model.CurrencyInfo
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import data.remote.api.CurrencyApi
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


    private lateinit var ivLogo: ImageView
    private lateinit var fadeOut: Animation
    private lateinit var fadeIn: Animation

    private lateinit var logoFrames: IntArray
    private var logoIndex = 0
    private val logoHandler = Handler(Looper.getMainLooper())

    private lateinit var logoRunnable: Runnable

    private val api: CurrencyApi by lazy {
        Retrofit.Builder().baseUrl("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/")
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(CurrencyApi::class.java)
    }

    private val prefs = "settings"
    private val keyCurrencyList = "currency_list"
    private val keyTheme = "theme_mode"

    override fun onCreate(savedInstanceState: Bundle?) {

        val prefs = getSharedPreferences(prefs, MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(
            prefs.getInt(keyTheme, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        )

        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSystemBarIconColors()

        val isDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        logoFrames = if (isDark) {
            intArrayOf(
                R.drawable.dollar_sign_red, R.drawable.euro_sign_red, R.drawable.yen_sign_red
            )
        } else {
            intArrayOf(
                R.drawable.dollar_sign_black, R.drawable.euro_sign_black, R.drawable.yen_sign_black
            )
        }

        val navView: NavigationView = findViewById(R.id.navViewContainer)
        ivLogo = navView.findViewById(R.id.ivLogo)

        fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        logoRunnable = Runnable {
            ivLogo.startAnimation(fadeOut.apply {
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationEnd(a: Animation?) {
                        logoIndex = (logoIndex + 1) % logoFrames.size
                        ivLogo.setImageResource(logoFrames[logoIndex])
                        ivLogo.startAnimation(fadeIn)
                        logoHandler.postDelayed(logoRunnable, 3_000L)
                    }

                    override fun onAnimationStart(a: Animation?) {}
                    override fun onAnimationRepeat(a: Animation?) {}
                })
            })
        }

        ivLogo.setImageResource(logoFrames[0])
        logoHandler.postDelayed(logoRunnable, 3_000L)


        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.isNavigationBarContrastEnforced = false
        }


        val nav = findViewById<NavigationView>(R.id.navViewContainer)
        nav.findViewById<SwitchMaterial>(R.id.switchDarkMode).apply {
            isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            setOnCheckedChangeListener { _, checked ->
                val mode =
                    if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                AppCompatDelegate.setDefaultNightMode(mode)
                prefs.edit().putInt(keyTheme, mode).apply()

                recreate()
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

        adapter = CurrencyAdapter(context = this,
            items = mutableListOf(),
            exchangeRates = emptyMap(),
            onCurrencyChangeRequested = { pos ->
                selectedBasePosition = pos
                showCurrencyBottomSheet { code -> changeCurrencyForItem(pos, code) }
            },
            onValueChanged = { basePos ->
                selectedBasePosition = basePos
                binding.currencyRecyclerView.post {
                    adapter.recalculateAll(basePos)
                }
            })
        binding.currencyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
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


        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.btnAdd.setOnClickListener { showCurrencyBottomSheet(::addNewCurrencyRow) }
        binding.btnCalculator.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }

        val spinAnim = AnimationUtils.loadAnimation(this, R.anim.spin)
        binding.btnUpdate.setOnClickListener { loadData(spinAnim) }
        binding.btnScrollRetry.setOnClickListener { loadData(spinAnim) }

        loadData(spinAnim)
    }


    private fun loadData(spinAnim: Animation) {
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
                    val prefs = getSharedPreferences(prefs, MODE_PRIVATE)
                    val saved = prefs.getString(keyCurrencyList, null)
                    val newList = mutableListOf<CurrencyItem>()
                    when {
                        saved == null -> { // first run
                            listOf("eur", "usd", "uah").forEach { code ->
                                fullCurrencyList.find { it.code == code }?.let {
                                    newList += CurrencyItem(code, it.name, eurRates[code] ?: 1.0)
                                }
                            }
                        }

                        saved.isEmpty() -> {}

                        else -> {
                            saved.split(",").forEach { code ->
                                fullCurrencyList.firstOrNull { it.code == code }?.let {
                                    newList += CurrencyItem(code, it.name, eurRates[code] ?: 1.0)
                                }
                            }
                        }
                    }

                    adapter.items.apply {
                        clear()
                        addAll(newList)
                    }
                    adapter.notifyDataSetChanged()

                    // toggle empty message
                    updateEmptyState()

                    if (adapter.items.isNotEmpty()) adapter.recalculateAll(0)

                    // timestamp
                    val now =
                        SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault()).format(Date())
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
        if (pos !in adapter.items.indices) return

        selectedBasePosition = pos

        val oldItem = adapter.items[pos]
        val oldRate = adapter.exchangeRates[oldItem.currency] ?: 1.0
        val oldEur = oldItem.value / oldRate

        val newRate = adapter.exchangeRates[newCode] ?: 1.0
        val newName = fullCurrencyList.firstOrNull { it.code == newCode }?.name ?: newCode

        adapter.updateItemCurrency(pos, newCode, newName, oldEur * newRate)
        adapter.recalculateAll(pos)
        saveCurrencyList()
    }


    private fun saveCurrencyList() {
        val csv = adapter.items.joinToString(",") { it.currency }
        getSharedPreferences(prefs, MODE_PRIVATE).edit().putString(keyCurrencyList, csv).apply()
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

    private fun setSystemBarIconColors() {
        val darkTheme =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
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
                sheetAdapter.updateData(fullCurrencyList.filter {
                    it.code.lowercase().contains(q) || it.name.lowercase().contains(q)
                })
            }
        })
        dlg.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        logoHandler.removeCallbacks(logoRunnable)
    }
}
