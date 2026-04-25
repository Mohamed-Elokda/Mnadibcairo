package com.example.myapplication.ui.transfer

import android.os.Bundle
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.R
import com.example.myapplication.domin.model.TransferDetailWithItemName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.collections.isNotEmpty
import kotlin.getValue

@AndroidEntryPoint
class TransferDetailsActivity : AppCompatActivity() {
    private val viewModel: TransferViewModel by viewModels()

    // تعريف الـ TableLayout للوصول إليه
    private lateinit var tableDetailItems: TableLayout
    private lateinit var tvDetailSupplier: TextView
    private lateinit var tvDetailInvoiceNum: TextView
    private lateinit var tvDetailDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transfer_details)

        // ربط الجدول
        tableDetailItems = findViewById(R.id.tableDetailItems)
        tvDetailSupplier = findViewById(R.id.tvDetailSupplier)
        tvDetailInvoiceNum = findViewById(R.id.tvDetailInvoiceNum)
        tvDetailDate = findViewById(R.id.tvDetailDate)

        setupInsets()

        val transferId = intent.getStringExtra("TRANSFER_ID")?:""
        val SUPPLIER = intent.getStringExtra("SUPPLIER")
        val transferNum = intent.getIntExtra("transferNum",0)
        val DATE = intent.getStringExtra("DATE")
        if (transferId.isNotEmpty()) {
            viewModel.loadTransferDetails(transferId)
            tvDetailSupplier.text="الي مخزن:"+SUPPLIER
            tvDetailInvoiceNum.text="رقم الفاتورة:"+transferNum
            tvDetailDate.text=DATE
            setupObservers() // استدعاء المراقب بعد تحميل البيانات
        } else {
            Toast.makeText(this, "خطأ في تحميل البيانات", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.details.collect { list ->
                    if (list.isNotEmpty()) {
                        renderTableRows(list) // دالة لرسم الصفوف داخل الجدول
                    }
                }
            }
        }
    }

    private fun renderTableRows(list: List<TransferDetailWithItemName>) {
        // مسح أي صفوف قديمة باستثناء صف العنوان (Header)
        val childCount = tableDetailItems.childCount
        if (childCount > 1) {
            tableDetailItems.removeViews(1, childCount - 1)
        }

        // إضافة الأصناف
        list.forEach { item ->
            val tableRow = TableRow(this).apply {
                setPadding(10, 10, 10, 10)
            }

            // نص اسم الصنف
            val tvName = TextView(this).apply {
                text = item.itemName
                gravity = Gravity.START
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(resources.getColor(R.color.black))
            }

            // نص الكمية
            val tvQty = TextView(this).apply {
                text = item.quantity.toString()
                gravity = Gravity.CENTER
                setTextColor(resources.getColor(R.color.black))
            }

            tableRow.addView(tvName)
            tableRow.addView(tvQty)

            tableDetailItems.addView(tableRow)
        }
    }

    private fun setupInsets() {
        // استخدم findViewById مباشرة جوه الـ Listener للتأكد إنه موجود
        val mainLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.mainTransferDetails)

        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }
}