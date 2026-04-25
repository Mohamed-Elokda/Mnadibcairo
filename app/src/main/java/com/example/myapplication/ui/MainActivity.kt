package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ui.outbound.OutboundActivity
import com.example.myapplication.R
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.data.repository.OutboundRepoImpl
import com.example.myapplication.data.repository.ReturnedRepoImpl
import com.example.myapplication.data.repository.StockRepoImpl
import com.example.myapplication.domin.repository.StockRepository
import com.example.myapplication.ui.customerState.CustomersActivity
import com.example.myapplication.ui.inbound.AddInboundActivity
import com.example.myapplication.ui.inbound.InboundActivity
import com.example.myapplication.ui.payment.PaymentActivity
import com.example.myapplication.ui.returned.ReturnedActivity
import com.example.myapplication.ui.store.StoresActivity
import com.example.myapplication.ui.transfer.TransferActivity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyA9FpMXbGtRAJt0jToeO65KkwcaIhocy8k" // ملاحظة: يفضل مستقبلاً وضع المفتاح في مكان آمن
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1. تهيئة العناصر
        val loadingOverlay = findViewById<FrameLayout>(R.id.loadingOverlay)
        val tvSyncStatus = findViewById<TextView>(R.id.tvSyncStatus)
        val userId = Prefs.getUserId(this) ?: ""

        // 2. تهيئة الـ Repositories والـ ViewModel

        val syncViewModel: SyncViewModel by viewModels ()

        // 3. مراقبة حالة المزامنة
        lifecycleScope.launch {
            syncViewModel.syncStatus.collect { state ->
                when (state) {
                    is SyncViewModel.SyncState.Loading -> {
                        loadingOverlay.visibility = View.VISIBLE
                    }
                    is SyncViewModel.SyncState.Progress -> {
                        loadingOverlay.visibility = View.VISIBLE
                        tvSyncStatus.text = state.step
                    }
                    is SyncViewModel.SyncState.Success -> {
                        loadingOverlay.visibility = View.GONE
                        Prefs.setFirstSyncDone(this@MainActivity, true) // حفظ نجاح المزامنة
                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is SyncViewModel.SyncState.Error -> {
                        loadingOverlay.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "خطأ: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }

        // 4. بدء المزامنة إذا كانت أول مرة (استخدام Prefs مباشرة)
        if (!Prefs.isFirstSyncDone(this)) {
            syncViewModel.startFullSync()
        }

        // 5. روابط الشاشات (الأزرار)
        setupClickListeners()

        // جدولة المزامنة التلقائية
        scheduleSync(this)

        // إعدادات الـ Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        findViewById<MaterialCardView>(R.id.cardInbound).setOnClickListener {
            startActivity(Intent(this, InboundActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardOutbound).setOnClickListener {
            startActivity(Intent(this, OutboundActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardStores).setOnClickListener {
            startActivity(Intent(this, StoresActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardCustomer).setOnClickListener {
            startActivity(Intent(this, CustomersActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.moneySupply).setOnClickListener {
            startActivity(Intent(this, PaymentActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.returned).setOnClickListener {
            startActivity(Intent(this, ReturnedActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.transform).setOnClickListener {
            startActivity(Intent(this, TransferActivity::class.java))
        }
    }
    private val speechRecognizerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            spokenText?.let { processWithGemini(it) } // نرسل النص لـ Gemini
        }
    }

    private fun startVoiceListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG") // اللهجة المصرية للمندوبين
            putExtra(RecognizerIntent.EXTRA_PROMPT, "قل تفاصيل الفاتورة (مثلاً: توريد 50 لمبة ليد 9 وات بسعر 20 جنيه)")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "جهازك لا يدعم التعرف على الصوت", Toast.LENGTH_SHORT).show()
        }
    }
    private fun processWithGemini(text: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val prompt = "تحليل النص التالي لعملية محاسبية: $text"
                generativeModel.generateContent(prompt)
            }

            withContext(Dispatchers.Main) {
                result.onSuccess { response ->
                    val cleanJson = response.text?.replace("```json", "")?.replace("```", "")?.trim()
                    if (!cleanJson.isNullOrEmpty()) handleAIResponse(cleanJson)
                }.onFailure { e ->
                    Log.e("GEMINI_ERROR", "Error details: ", e)
                    Toast.makeText(this@MainActivity, "خطأ في الاتصال: تأكد من تشغيل الإنترنت", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun handleAIResponse(json: String) {
        try {
            val jsonObject = JSONObject(json)
            val action = jsonObject.getString("action")
            val itemsArray = jsonObject.getJSONArray("items")

            // تحديد الشاشة الهدف
            val targetActivity = if (action == "INBOUND") {
                AddInboundActivity::class.java
            } else {
                OutboundActivity::class.java // عدلها حسب اسم شاشة الصادر عندك
            }

            val intent = Intent(this, targetActivity).apply {
                putExtra("AI_ITEMS_JSON", itemsArray.toString())
                putExtra("FROM_AI", true)
            }
            startActivity(intent)

        } catch (e: Exception) {
            Log.e("JSON_ERROR", "Parsing error: ${e.message}")
            Toast.makeText(this, "لم أفهم تفاصيل الفاتورة بوضوح", Toast.LENGTH_SHORT).show()
        }
    }
}