package com.example.myapplication.ui.returned

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.ReturnedRepoImpl
import com.example.myapplication.domin.useCase.AddReturnedUseCase
import com.example.myapplication.domin.useCase.GetAllCustomersUseCase
import com.example.myapplication.domin.useCase.GetAllReturnedUseCase
import com.example.myapplication.domin.useCase.GetCustomerItemsUseCase
import com.example.myapplication.domin.useCase.GetItemHistoryUseCase
import com.example.myapplication.domin.useCase.GetLastPriceUseCase
import com.example.myapplication.domin.useCase.GetReturnedDetailsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
@AndroidEntryPoint
class ReturnedDetailsActivity : AppCompatActivity() {

    private val viewModel: ReturnedViewModel by viewModels ()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_returned_details)

        val returnedId = intent.getStringExtra("RETURNED_ID")?:""
        val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: "غير معروف"
        val date = intent.getStringExtra("DATE") ?: ""

        // ربط البيانات الأساسية
        findViewById<TextView>(R.id.tvDetCustomerName).text = "العميل: $customerName"
        findViewById<TextView>(R.id.tvDetDate).text = "التاريخ: $date"
        findViewById<TextView>(R.id.tvDetInvoiceId).text = "رقم العملية: #$returnedId"

        val rv = findViewById<RecyclerView>(R.id.rvReturnedDetails)
        val adapter = ReturnedDetailsAdapter() // ستحتاج لإنشاء هذا الـ Adapter
        rv.adapter = adapter

        // جلب البيانات
        if (returnedId.isNotEmpty()) {
            lifecycleScope.launch {
                viewModel.loadReturnedDetails(returnedId) // تأكد من إضافة هذه الدالة في الـ ViewModel
                viewModel.returnedDetails.collect { details ->
                    adapter.submitList(details)
                    // حساب الإجمالي
                    val total = details.sumOf { it.price * it.amount }
                    findViewById<TextView>(R.id.tvTotalReturnAmount).text = String.format("%.2f ج.م", total)
                }
            }
        }
    }
}