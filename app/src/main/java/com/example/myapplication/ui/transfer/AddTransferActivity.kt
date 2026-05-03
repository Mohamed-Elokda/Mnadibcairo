package com.example.myapplication.ui.transfer

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entity.Supplied
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferDetails
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.data.repository.TransferRepositoryImpl
import com.example.myapplication.domin.repository.IInboundRepository
import com.example.myapplication.domin.useCase.AddTransferUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddTransferActivity : AppCompatActivity() {

    private val viewModel: TransferViewModel by viewModels ()

    // قائمة مؤقتة للأصناف المضافة للفاتورة
    private val transferItemsList = mutableListOf<TransferDetails>()

    // المتغيرات المختارة
    private var selectedToStoreId: Int = -1
    private var selectedItem: Items? = null
    private var userStoreId: Int = -1

    // عناصر الواجهة
    private lateinit var tableItems: TableLayout
    private lateinit var toAutoSupplier: AutoCompleteTextView
    private lateinit var autoItem: AutoCompleteTextView
    private lateinit var etQuantity: EditText
    private lateinit var etInvoiceNumber: EditText
    private lateinit var btnAddItem: Button
    private lateinit var btnSave: Button
    private var isEditMode = false
    private var editTransferId = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_transfer)

        initViews()
        setupObservers()


// جوه onCreate
        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        if (isEditMode) {
            editTransferId = intent.getIntExtra("TRANSFER_ID", -1)
//            loadTransferDataForEdit(editTransferId)
            btnSave.text = "تحديث المناقلة" // نغير اسم الزرار عشان يبقى منطقي
        }
        // زر إضافة صنف للقائمة (الجدول)
        btnAddItem.setOnClickListener {
            addItemToTable()
        }

        // زر الحفظ النهائي
        btnSave.setOnClickListener {
            saveTransferInvoice()
        }
    }

    private fun initViews() {
        tableItems = findViewById(R.id.tableItems)
        toAutoSupplier = findViewById(R.id.fromAutoSupplier)
        autoItem = findViewById(R.id.autoItem)
        etQuantity = findViewById(R.id.etQuantity)
        etInvoiceNumber = findViewById(R.id.etInvoiceNumber)
        btnAddItem = findViewById(R.id.btnAddItem)
        btnSave = findViewById(R.id.btnSave)
    }

//    private fun loadTransferDataForEdit(id: Int) {
//        viewModel.getTransferById(id).observe(this) { transfer ->
//            etInvoiceNumber.setText(transfer.transferNum.toString())
//            // هنا هتحتاج تعمل Logic عشان تختار المخزن الصح في الـ AutoComplete
//        }
//
//        viewModel.loadTransferDetails(id).observe(this) { details ->
//            // هنا تلف على التفاصيل وتضيفها للـ transferItemsList وللـ tableItems
//            details.forEach { detail ->
//                // نستخدم نفس الدالة اللي عملناها قبل كدة لإضافة الصفوف
//                addExistingItemToTable(detail)
//            }
//        }
//    }

    private fun setupObservers() {
        // مراقبة قائمة المخازن (الوجهة)
        viewModel.allSupplied.observe(this) { suppliedList ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suppliedList.map { it.suppliedName })
            toAutoSupplier.setAdapter(adapter)
            toAutoSupplier.setOnItemClickListener { _, _, position, _ ->
                selectedToStoreId = suppliedList[position].id
            }
        }

        // مراقبة قائمة الأصناف
        // مراقبة قائمة الأصناف
        viewModel.allItems.observe(this) { itemsList ->
            // الأديبتر بياخد Strings بس عشان يعرض الأسماء
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, itemsList.map { it.itemName })
            autoItem.setAdapter(adapter)

            autoItem.setOnItemClickListener { _, _, position, _ ->
                // بدل الـ Cast، بنجيب الكائن من القائمة الأصلية باستخدام الترتيب (position)

                 val selectedName = adapter.getItem(position)
                 selectedItem = itemsList.find { it.itemName == selectedName }
            }
        }

        // مراقبة حالة الحفظ
        viewModel.status.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            if (message.contains("بنجاح")) finish()
        }
    }

    private fun addItemToTable() {
        val qtyStr = etQuantity.text.toString()
        val itemName = autoItem.text.toString()

        if (selectedItem == null || qtyStr.isEmpty()) {
            Toast.makeText(this, "يرجى اختيار صنف وتحديد الكمية", Toast.LENGTH_SHORT).show()
            return
        }

        val qty = qtyStr.toInt()
        val userId = Prefs.getUserId(this) ?: ""

        // إضافة للـ List البرمجية
        transferItemsList.add(TransferDetails(id = java.util.UUID.randomUUID().toString(), transferId = "", itemId = selectedItem!!.id, amount = qty))

        // إضافة للـ TableLayout (الواجهة)
        val row = TableRow(this)
        row.setPadding(0, 10, 0, 10)

        val tvName = TextView(this).apply { text = itemName; layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f) }
        val tvQty = TextView(this).apply { text = qty.toString(); gravity = android.view.Gravity.CENTER }
        val btnDelete = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnClickListener {
                tableItems.removeView(row)
                transferItemsList.removeAll { it.itemId == selectedItem!!.id && it.amount == qty }
            }
        }

        row.addView(tvName)
        row.addView(tvQty)
        row.addView(btnDelete)
        tableItems.addView(row)

        // تصفية الحقول للإضافة التالية
        autoItem.text.clear()
        etQuantity.text.clear()
        selectedItem = null
    }

    private fun saveTransferInvoice() {
        val invoiceNum = etInvoiceNumber.text.toString()

        if (invoiceNum.isEmpty()) {
            etInvoiceNumber.error = "مطلوب"
            return
        }

        if (transferItemsList.isEmpty()) {
            Toast.makeText(this, "القائمة فارغة!", Toast.LENGTH_SHORT).show()
            return
        }

        val transfer = Transfer(
            id = java.util.UUID.randomUUID().toString(),
            transferNum = invoiceNum.toInt(),
            fromStoreId = Prefs.getUserId(this@AddTransferActivity)?:"",
            toStoreId = selectedToStoreId,
            date =  SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date()),
            userId = Prefs.getUserId(this) ?: "",
            isSynced = false
        )

        viewModel.executeTransfer(transfer, transferItemsList)
    }
}