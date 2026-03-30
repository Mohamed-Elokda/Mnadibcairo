package com.example.myapplication.ui.transfer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import androidx.core.widget.doOnTextChanged
import com.example.myapplication.data.local.Prefs

class TransferActivity : AppCompatActivity() {

    // افترضنا وجود TransferViewModel تم إعداده بـ Factory
//    private val viewModel: TransferViewModel by viewModels { /* Factory */ }

    private lateinit var rvTransfers: RecyclerView
    private lateinit var btnAddTransfer: FloatingActionButton
    private lateinit var etSearch: TextInputEditText

    // الأدايبتر الخاص بعرض المناقلات في القائمة
    // ستحتاج لإنشاء TransferAdapter لاحقاً
    // private lateinit var adapter: TransferAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transfer)

        setupInsets()
        initViews()
        setupRecyclerView()
        setupObservers()

        // الانتقال لصفحة إضافة مناقلة جديدة
        btnAddTransfer.setOnClickListener {
            startActivity(Intent(this, AddTransferActivity::class.java))
        }

        // منطق البحث
        etSearch.doOnTextChanged { text, _, _, _ ->
            // filterList(text.toString())
        }
    }

    private fun initViews() {
        rvTransfers = findViewById(R.id.rvReturnedInvoices) // استخدمنا نفس ID الـ XML الخاص بك
        btnAddTransfer = findViewById(R.id.btnAddReturn)    // زر الإضافة (FAB)
        etSearch = findViewById(R.id.etSearchReturned)      // حقل البحث
    }

    private fun setupRecyclerView() {
        rvTransfers.layoutManager = LinearLayoutManager(this)
        // adapter = TransferAdapter()
        // rvTransfers.adapter = adapter
    }

    private fun setupObservers() {
        val userId = Prefs.getUserId(this) ?: ""

        // جلب قائمة المناقلات من الـ ViewModel
//        viewModel.getTransfers(userId).observe(this) { list ->
//            if (list.isEmpty()) {
//                // يمكنك إظهار صورة "لا توجد بيانات" هنا
//            } else {
//                // adapter.submitList(list)
//            }
//        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}