package com.example.myapplication.ui.transfer

import android.app.AlertDialog
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
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.data.repository.TransferRepositoryImpl
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferWithStoreName
import com.example.myapplication.domin.useCase.AddTransferUseCase
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransferActivity : AppCompatActivity() {

    private val viewModel: TransferViewModel by viewModels ()
    private lateinit var adapter: TransferAdapter

    private lateinit var rvTransfers: RecyclerView
    private lateinit var btnAddTransfer: ExtendedFloatingActionButton
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

        adapter = TransferAdapter(
            onItemClick = { transfer ->
                val intent = Intent(this, TransferDetailsActivity::class.java).apply {
                    putExtra("TRANSFER_ID", transfer.id)
                    putExtra("SUPPLIER", transfer.toStoreName)
                    putExtra("transferNum", transfer.transferNum)
                    putExtra("DATE", transfer.date)
                }
                startActivity(intent)
            },
            onLongClick = { transfer ->
                showOptionsDialog(transfer) // نفتح قائمة الخيارات
                true
            },

        )

        rvTransfers.adapter = adapter
    }
    private fun showOptionsDialog(transfer: TransferWithStoreName) {
        val options = arrayOf("تعديل المناقلة", "حذف المناقلة")

        AlertDialog.Builder(this)
            .setTitle("خيارات المناقلة رقم ${transfer.transferNum}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editTransfer(transfer)   // اختيار التعديل
                    1 -> confirmDelete(transfer) // اختيار الحذف
                }
            }
            .show()
    }

    private fun editTransfer(transfer: TransferWithStoreName) {
        // هنا تفتح صفحة الإضافة لكن تمرر لها البيانات عشان تتعدل
        val intent = Intent(this, AddTransferActivity::class.java)
        intent.putExtra("IS_EDIT_MODE", true)
        intent.putExtra("TRANSFER_ID", transfer.id)
        startActivity(intent)
    }

    private fun confirmDelete(transfer: TransferWithStoreName) {
        AlertDialog.Builder(this)
            .setTitle("تأكيد الحذف")
            .setMessage("هل أنت متأكد من حذف المناقلة رقم ${transfer.transferNum}؟")
            .setPositiveButton("حذف") { _, _ ->
                viewModel.deleteTransfer(transfer.id) // نكلم الـ ViewModel يحذفها
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun setupObservers() {
        val userId = Prefs.getUserId(this) ?: ""

        viewModel.getTransfers(userId).observe(this) { list ->
            if (list.isEmpty()) {
                // يمكنك إظهار TextView مكتوب عليه "لا توجد مناقلات"
            } else {
                adapter.submitList(list)
            }
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}