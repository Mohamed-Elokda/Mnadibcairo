package com.example.myapplication.ui.returned

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.local.entity.ItemsEntity
import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.ReturnedRepoImpl
import com.example.myapplication.domin.model.Customer
import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.repository.ReturnedRepo
import com.example.myapplication.domin.useCase.AddReturnedUseCase
import com.example.myapplication.domin.useCase.GetAllCustomersUseCase
import com.example.myapplication.domin.useCase.GetAllReturnedUseCase
import com.example.myapplication.domin.useCase.GetCustomerItemsUseCase
import com.example.myapplication.domin.useCase.GetItemHistoryUseCase
import com.example.myapplication.domin.useCase.GetLastPriceUseCase
import com.example.myapplication.domin.useCase.GetReturnedDetailsUseCase
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddReturnedActivity : AppCompatActivity() {

    private val tempItemsList = mutableListOf<ReturnedDetailsModel>()
    private var totalInvoiceAmount = 0.0

    // تعريف المتغيرات على مستوى الكلاس
    private lateinit var autoCustomer: AutoCompleteTextView
    private lateinit var autoItem: AutoCompleteTextView
    private lateinit var etQuantity: EditText
    private lateinit var etSellingPrice: EditText
    private lateinit var tvTotalInvoice: TextView
    private lateinit var tableItems: TableLayout

    private lateinit var customerAdapter: ArrayAdapter<String>
    private lateinit var itemAdapter: ArrayAdapter<String>

    private var currentCustomers = listOf<Customer>()
    private var currentItems = listOf<ItemsEntity>()
    private var selectedCustomerId: Int = -1

    private val viewModel: ReturnedViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = ReturnedRepoImpl(
            database,
            database.returnedDao(),
            database.returnedDetailsDao(),
            database.outboundDetailesDao(),
            database.customerDao(),
            database.stockDao()
        )
        val addUseCase = AddReturnedUseCase(repository)
        val customerRepo = CustomerRepoImpl(database.customerDao())
        val getAllReturnedUseCase = GetAllReturnedUseCase(repository)
        val getAllCustomersUseCase = GetAllCustomersUseCase(customerRepo)
        val getCustomerItemsUseCase = GetCustomerItemsUseCase(repository)
        val getLastPriceUseCase = GetLastPriceUseCase(repository)
        val getReturnedDetailsUseCase = GetReturnedDetailsUseCase(repository)
        val getItemHistoryUseCas = GetItemHistoryUseCase(repository)


        // تأكد أن الـ Factory يستقبل الـ UseCases الجديدة التي صممناها
        ReturnedViewModelFactory(
            getCustomerItemsUseCase,
            getLastPriceUseCase,
            getAllReturnedUseCase,
            getReturnedDetailsUseCase,
            addUseCase,
            getAllCustomersUseCase,
            getItemHistoryUseCas
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_returned)

        // ربط العناصر بالواجهة (بدون إعادة تعريف val)
        autoItem = findViewById(R.id.autoItem)
        autoCustomer = findViewById(R.id.autoCustomer)
        etQuantity = findViewById(R.id.etQuantity)
        etSellingPrice = findViewById(R.id.etSellingPrice)
        tvTotalInvoice = findViewById(R.id.tvTotalInvoice)
        tableItems = findViewById(R.id.tableItems)

        val btnAddItem = findViewById<Button>(R.id.btnAddItem)
        val btnSave = findViewById<Button>(R.id.btnSave)

        setupObservers()
        setupListeners(btnAddItem, btnSave)
    }

    private fun setupObservers() {
        // 1. مراقبة العملاء
        lifecycleScope.launch {
            viewModel.allCustomers(this@AddReturnedActivity).collect { customers ->
                currentCustomers = customers
                val names = customers.map { it.customerName }
                customerAdapter = ArrayAdapter(
                    this@AddReturnedActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                autoCustomer.setAdapter(customerAdapter)
            }
        }

        // 2. مراقبة الأصناف (تتحدث عند اختيار العميل)
        lifecycleScope.launch {
            viewModel.customerItems.collect { items ->
                currentItems = items
                val names = items.map { it.itemName }
                itemAdapter = ArrayAdapter(
                    this@AddReturnedActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                autoItem.setAdapter(itemAdapter)
            }
        }
        findViewById<ImageButton>(R.id.btnShowItemHistory).setOnClickListener {
            val selectedItemName = autoItem.text.toString()
            val itemObject = currentItems.find { it.itemName == selectedItemName }

            if (selectedCustomerId != -1 && itemObject != null) {
                // 1. طلب البيانات من الـ ViewModel
                viewModel.loadItemHistory(selectedCustomerId, itemObject.id)

                // 2. عرض الـ BottomSheet (كما في الكود السابق)
                showHistoryDialog(itemObject.itemName)
            }
        }
        // 3. مراقبة سعر آخر بيع
        lifecycleScope.launch {
            viewModel.lastPrice.collect { price ->
                etSellingPrice.setText(price.toString())
            }
        }
    }

    private fun setupListeners(btnAddItem: Button, btnSave: Button) {
        // عند اختيار عميل
        autoCustomer.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position).toString()
            val customer = currentCustomers.find { it.customerName == selectedName }

            selectedCustomerId = customer?.id ?: -1

            if (selectedCustomerId != -1) {
                viewModel.loadItemsForCustomer(selectedCustomerId)
                autoItem.setText("")
            }
        }
        // عند اختيار صنف
        autoItem.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = currentItems[position]
            viewModel.loadLastPrice(selectedCustomerId, selectedItem.id) // جلب آخر سعر بيع
        }

        // زر إضافة صنف للجدول
        btnAddItem.setOnClickListener {
            val itemName = autoItem.text.toString()
            val qty = etQuantity.text.toString().toIntOrNull() ?: 0
            val price = etSellingPrice.text.toString().toDoubleOrNull() ?: 0.0

            // نحتاج للوصول للـ ID الخاص بالصنف المختار
            val selectedItemName = autoItem.text.toString()
            val itemObject = currentItems.find { it.itemName == selectedItemName }

            if (itemObject != null && qty > 0) {
                val rowTotal = qty * price

                // 1. الإضافة للقائمة البرمجية (هذا ما كان ينقصك)
                tempItemsList.add(
                    ReturnedDetailsModel(
                        id = 0,
                        returnedId = 0, // سيتحدد تلقائياً في الـ Room
                        itemId = itemObject.id,
                        amount = qty,
                        price = price,
                        itemName = ""
                    )
                )

                // 2. الإضافة للجدول (UI)
                addRowToTable(tableItems, itemName, qty, rowTotal)

                totalInvoiceAmount += rowTotal
                tvTotalInvoice.text = String.format("%.2f ج.م", totalInvoiceAmount)

                clearInputFields()
            } else {
                Toast.makeText(this, "يرجى اختيار صنف صحيح وكمية", Toast.LENGTH_SHORT).show()
            }
        }
        // زر حفظ الفاتورة بالكامل
        btnSave.setOnClickListener {
            Toast.makeText(this@AddReturnedActivity, "clicked", Toast.LENGTH_LONG).show()
            if (selectedCustomerId != -1 && tempItemsList.isNotEmpty()) {
                Toast.makeText(this@AddReturnedActivity, "good", Toast.LENGTH_LONG).show()

                val paidAmount =
                    findViewById<EditText>(R.id.etPaidAmount).text.toString().toDoubleOrNull()
                        ?: 0.0
                val debtAmount = totalInvoiceAmount - paidAmount

                val returnedModel = ReturnedModel(
                    id = 0,
                    customerId = selectedCustomerId,
                    returnedDate = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    ).format(Date()),
                    itemId = 0,
                    latitude = 0.0,
                    longitude = 0.0,
                    userId = Prefs.getUserId(this@AddReturnedActivity)?:"",
                )

                viewModel.addReturned(returnedModel, tempItemsList, debtAmount)
                finish()
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showHistoryDialog(itemName: String) {
        // 1. إنشاء الـ BottomSheetDialog
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_item_history_bottom_sheet, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvHistoryTitle)
        val container = view.findViewById<LinearLayout>(R.id.historyContainer)

        tvTitle.text = "سجل شراء: $itemName"

        // 2. مراقبة البيانات القادمة من الـ ViewModel
        lifecycleScope.launch {
            viewModel.itemHistory.collectLatest { historyList ->
                container.removeAllViews() // مسح البيانات القديمة قبل العرض الجديد

                if (historyList.isEmpty()) {
                    val emptyTv = TextView(this@AddReturnedActivity).apply {
                        text = "لا توجد عمليات بيع سابقة لهذا العميل لهذا الصنف"
                        gravity = Gravity.CENTER
                        setPadding(0, 50, 0, 50)
                    }
                    container.addView(emptyTv)
                } else {
                    historyList.forEach { history ->
                        // تصميم شكل السطر (يمكنك تحسينه بـ Custom Layout)
                        val historyView =
                            layoutInflater.inflate(R.layout.item_history_row, container, false)
                        historyView.findViewById<TextView>(R.id.tvHistoryDate).text = history.date
                        historyView.findViewById<TextView>(R.id.tvHistoryQty).text =
                            "الكمية: ${history.amount}"
                        historyView.findViewById<TextView>(R.id.tvHistoryPrice).text =
                            "السعر: ${history.price} ج.م"

                        // ميزة إضافية: عند الضغط على السطر يتم وضع السعر في الحقل الرئيسي
                        historyView.setOnClickListener {
                            etSellingPrice.setText(history.price.toString())
                            bottomSheet.dismiss() // إغلاق النافذة
                        }

                        container.addView(historyView)
                    }
                }
            }
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    private fun addRowToTable(table: TableLayout, name: String, qty: Int, total: Double) {
        val row = TableRow(this)
        row.setPadding(8, 8, 8, 8)

        val tvName = TextView(this).apply {
            text = name; layoutParams =
            TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvQty = TextView(this).apply { text = qty.toString(); gravity = Gravity.CENTER }
        val tvTotal =
            TextView(this).apply { text = String.format("%.2f", total); gravity = Gravity.CENTER }

        val btnDelete = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                table.removeView(row)
                updateTotalAfterDelete(total)
            }
        }

        row.addView(tvName)
        row.addView(tvQty)
        row.addView(tvTotal)
        row.addView(btnDelete)
        table.addView(row)
    }

    private fun updateTotalAfterDelete(amountToSubtract: Double) {
        totalInvoiceAmount -= amountToSubtract
        if (totalInvoiceAmount < 0) totalInvoiceAmount = 0.0
        tvTotalInvoice.text = String.format("%.2f ج.م", totalInvoiceAmount)
    }

    private fun clearInputFields() {
        autoItem.setText("")
        etQuantity.setText("")
        etSellingPrice.setText("")
    }
}