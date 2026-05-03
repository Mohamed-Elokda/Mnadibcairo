package com.example.myapplication.ui.payment

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.PaymentRepositoryImpl
import com.example.myapplication.domin.model.PaymentItem
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue
@AndroidEntryPoint
class PaymentActivity : AppCompatActivity() {
    private lateinit var adapter: PaymentsAdapter
    private val viewModel: PaymentsViewModel by viewModels ()

    private var fullList = listOf<PaymentItem>() // القائمة الأصلية من قاعدة البيانات
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_supply)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        scheduleSync(context = this)

        val rvPayments = findViewById<RecyclerView>(R.id.rvPayments)
        val etSearch = findViewById<EditText>(R.id.etSearchPayment)
        val btnAdd = findViewById<ImageButton>(R.id.btnAddPayment)
        viewModel.loadPayments()
        // إعداد الـ RecyclerView
        adapter = PaymentsAdapter { payment ->
            // حدث عند الضغط على عنصر (مثلاً عرض التفاصيل)
        }
        rvPayments.layoutManager = LinearLayoutManager(this)
        rvPayments.adapter = adapter

        // مراقبة الـ ViewModel (مثال)
         viewModel.allPayments.observe(this) { list ->
            fullList = list
            adapter.submitList(list)
         }

        // منطق البحث
        etSearch.addTextChangedListener { text ->
            filterList(text.toString())
        }

        btnAdd.setOnClickListener {
            // الانتقال لشاشة إضافة توريد جديد
            startActivity(Intent(this, AddPaymentActivity::class.java))
        }
    }

    private fun filterList(query: String) {
        val filtered = fullList.filter {
            it.customerName.contains(query, ignoreCase = true)
        }
        adapter.submitList(filtered)
    }

}