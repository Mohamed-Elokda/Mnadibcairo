package com.example.myapplication.ui.payment

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.PaymentRepositoryImpl
import com.example.myapplication.domin.repository.CustomerRepo
import com.google.android.material.button.MaterialButtonToggleGroup

class AddPaymentActivity : AppCompatActivity() {

    private var selectedCustomerId: Int? = null
    private var selectedType = "نقدي"
    private val viewModel: PaymentsViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)

        // إنشاء الـ Repository (تأكد من مطابقة المعاملات للـ Constructor الخاص به)
        val repository = PaymentRepositoryImpl(
            database,
            database.PaymentDao(),
            database.customerDao()
        )
        val Customerrepository = CustomerRepoImpl(
            database.customerDao(),
        )

        // استخدام الفاكتوري (سنقوم بإنشائه في الخطوة التالية)
        PaymentsViewModelFactory(repository,Customerrepository)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_payment)

        val autoCustomer = findViewById<AutoCompleteTextView>(R.id.autoCompleteCustomer)
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.togglePaymentType)
        val btnSave = findViewById<Button>(R.id.btnConfirmPayment)

        // 1. جلب العملاء وربطهم بالـ Adapter
        viewModel.allCustomers(this).observe(this) { customers ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, customers)
            autoCustomer.setAdapter(adapter)
            autoCustomer.setOnItemClickListener { _, _, position, _ ->
                selectedCustomerId = adapter.getItem(position)?.id
            }
        }

        // 2. متابعة اختيار نوع الدفع
        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                selectedType = when (checkedId) {
                    R.id.btnVodafone -> "فودافون كاش"
                    R.id.btnInstaPay -> "إنستا باي"
                    else -> "نقدي"
                }
            }
        }

        // 3. حفظ البيانات
        btnSave.setOnClickListener {
            val amount = findViewById<EditText>(R.id.etPaymentAmount).text.toString().toDoubleOrNull()

            if (selectedCustomerId == null || amount == null || amount <= 0) {
                Toast.makeText(this, "يرجى إكمال البيانات بشكل صحيح", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // تنفيذ الحفظ وتحديث رصيد العميل في الـ ViewModel
            viewModel.savePayment(selectedCustomerId!!, amount, selectedType)
            finish() // العودة للخلف بعد النجاح
        }
    }
}