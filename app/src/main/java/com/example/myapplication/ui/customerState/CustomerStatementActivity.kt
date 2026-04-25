package com.example.myapplication.ui.customerState

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.StatementRepoImpl
import com.example.myapplication.domin.useCase.GetCustomerStatementUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
@AndroidEntryPoint
class CustomerStatementActivity : AppCompatActivity() {

    private lateinit var statementAdapter: StatementAdapter
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvCustomerName: TextView

    // 1. إكمال تعريف الـ ViewModel مع الـ Factory
    private val viewModel: StatementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_statement)

        // إعداد الـ Window Insets (Padding للمسافات الآمنة)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 2. ربط العناصر بالواجهة
        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        tvCustomerName = findViewById(R.id.tvCustomerName)

        // جلب البيانات من الـ Intent (الاسم والـ ID)
        val customerId = intent.getIntExtra("CUSTOMER_ID", -1)
        val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: "عميل غير معروف"

        tvCustomerName.text = customerName

        // 3. تهيئة الـ RecyclerView والمراقبة
        setupRecyclerView()
        observeViewModel()

        // 4. طلب البيانات من الـ ViewModel
        if (customerId != -1) {
            viewModel.loadStatement(customerId)
        }
    }

    private fun setupRecyclerView() {
        statementAdapter = StatementAdapter()
        findViewById<RecyclerView>(R.id.rvStatement).apply {
            adapter = statementAdapter
            layoutManager = LinearLayoutManager(this@CustomerStatementActivity)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.statementState.collect { list ->
                // تحديث القائمة في الـ Adapter
                statementAdapter.submitList(list)

                // تحديث إجمالي المديونية في الكارت العلوي (آخر رصيد في القائمة التراكمية)
                if (list.isNotEmpty()) {
                    val finalBalance = list.last().runningBalance
                    tvTotalBalance.text = String.format("إجمالي المديونية: %.2f ج.م", finalBalance)
                } else {
                    tvTotalBalance.text = "لا توجد حركات مالية"
                }
            }
        }
    }
}