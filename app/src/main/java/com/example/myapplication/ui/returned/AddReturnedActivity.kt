package com.example.myapplication.ui.returned

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.local.entity.ItemsEntity
import com.example.myapplication.domin.model.CustomerModel
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
class AddReturnedActivity : AppCompatActivity() {

    private val tempItemsList = mutableListOf<ReturnedDetailsModel>()
    private var totalInvoiceAmount = 0.0
    private var currentReturnedId: String = "" // لتخزين ID الفاتورة في حالة التعديل
    private var selectedItem: Items? = null

    private lateinit var autoCustomer: AutoCompleteTextView
    private lateinit var autoItem: AutoCompleteTextView
    private lateinit var etQuantity: EditText
    private lateinit var etInvoiceNumber: EditText
    private lateinit var etSellingPrice: EditText
    private lateinit var tvTotalInvoice: TextView
    private lateinit var tableItems: TableLayout
    private lateinit var btnSave: Button

    private lateinit var customerAdapter: ArrayAdapter<String>
    private lateinit var itemAdapter: ArrayAdapter<String>

    private var currentCustomerModels = listOf<CustomerModel>()
    private var currentItems = listOf<Items>()
    private var selectedCustomerId: Int = -1

    private val viewModel: ReturnedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_returned)

        initViews()
        setupObservers()
        handleEditMode()
        setupListeners()
    }

    private fun initViews() {
        autoItem = findViewById(R.id.autoItem)
        etInvoiceNumber = findViewById(R.id.etInvoiceNumber)
        autoCustomer = findViewById(R.id.autoCustomer)
        etQuantity = findViewById(R.id.etQuantity)
        etSellingPrice = findViewById(R.id.etSellingPrice)
        tvTotalInvoice = findViewById(R.id.tvTotalInvoice)
        tableItems = findViewById(R.id.tableItems)
        btnSave = findViewById(R.id.btnSave)
    }

    @SuppressLint("DefaultLocale")
    private fun handleEditMode() {
        val isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        if (isEditMode) {
            currentReturnedId = intent.getStringExtra("RETURNED_ID")?:""
            val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: ""
            selectedCustomerId = intent.getIntExtra("CUSTOMER_ID", -1)

            autoCustomer.setText(customerName)
            btnSave.text = "تحديث المرتجع"

            // تحميل التفاصيل من قاعدة البيانات
            viewModel.loadReturnedDetails(currentReturnedId)

            lifecycleScope.launch {
                viewModel.returnedDetails.collect { details ->
                    if (details.isNotEmpty() && tempItemsList.isEmpty()) {
                        tableItems.removeAllViews()
                        tempItemsList.clear()
                        tempItemsList.addAll(details)
                        totalInvoiceAmount = 0.0

                        details.forEach { detail ->
                            val rowTotal = detail.amount * detail.price
                            addRowToTable(tableItems,
                                detail.itemName, detail.amount, rowTotal,detail.price)
                            totalInvoiceAmount += rowTotal
                        }
                        tvTotalInvoice.text = String.format("%.2f ج.م", totalInvoiceAmount)
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        // 1. مراقبة العملاء
        lifecycleScope.launch {
            viewModel.allCustomers().collect { customers ->
                currentCustomerModels = customers
                val names = customers.map { it.customerName }
                customerAdapter = ArrayAdapter(this@AddReturnedActivity, android.R.layout.simple_dropdown_item_1line, names)
                autoCustomer.setAdapter(customerAdapter)
            }
        }

        // 2. مراقبة كل الأصناف (تظهر فوراً بدون شرط العميل)

            viewModel.allItems.observe(this) { items ->
                if (items.isNotEmpty() && ::autoItem.isInitialized) {
                    currentItems = items
                    val adapter = object : ArrayAdapter<Items>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        ArrayList(items) // نسخة قابلة للتغيير
                    ) {

                        private val fullList = ArrayList(items) // النسخة الأصلية

                        override fun getFilter(): Filter {
                            return object : Filter() {

                                override fun performFiltering(constraint: CharSequence?): FilterResults {
                                    val results = FilterResults()

                                    val filteredList = if (constraint.isNullOrBlank()) {
                                        fullList
                                    } else {
                                        val searchTerms = constraint.toString().split("+")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }

                                        fullList.filter { item ->
                                            searchTerms.all { term ->
                                                item.itemName.contains(term, ignoreCase = true)
                                            }
                                        }
                                    }

                                    results.values = filteredList
                                    results.count = filteredList.size
                                    return results
                                }

                                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                                    clear()
                                    if (results != null) {
                                        addAll(results.values as List<Items>)
                                    }
                                    notifyDataSetChanged()
                                }
                            }
                        }
                    }
                    autoItem.setAdapter(adapter)
                    autoItem.setOnClickListener { autoItem.showDropDown() }
                    autoItem.setOnItemClickListener { parent, _, position, _ ->
                        val selected = parent.getItemAtPosition(position) as Items
                        selectedItem= selected
                    }
                }

        }

        // 3. مراقبة حالة الحفظ
        lifecycleScope.launch {
            viewModel.saveStatus.collect { _ ->

                        Toast.makeText(this@AddReturnedActivity, "تمت العملية بنجاح", Toast.LENGTH_SHORT).show()
                        finish()


            }
        }

        // مراقبة سعر آخر بيع وسجل الشراء
        lifecycleScope.launch { viewModel.lastPrice.collect { price -> etSellingPrice.setText(price.toString()) } }
    }

    private fun setupListeners() {
        val btnAddItem = findViewById<Button>(R.id.btnAddItem)

        autoCustomer.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position).toString()
            val customer = currentCustomerModels.find { it.customerName == selectedName }
            selectedCustomerId = customer?.id ?: -1
        }

        autoItem.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = currentItems[position]
            if (selectedCustomerId != -1) {
                viewModel.loadLastPrice(selectedCustomerId, selectedItem.id)
            }
        }

        btnAddItem.setOnClickListener {
            addNewItemToTable()
        }

        btnSave.setOnClickListener {
            saveInvoice()
        }

        findViewById<ImageButton>(R.id.btnShowItemHistory).setOnClickListener {
            showHistoryIfPossible()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun addNewItemToTable() {
        val itemName = autoItem.text.toString()
        val qty = etQuantity.text.toString().toIntOrNull() ?: 0
        val price = etSellingPrice.text.toString().toDoubleOrNull() ?: 0.0
        val itemObject = currentItems.find { it.itemName == itemName }

        if (itemObject != null && qty > 0) {
            val rowTotal = qty * price
            tempItemsList.add(
                ReturnedDetailsModel(
                UUID.randomUUID().toString(),
                    currentReturnedId,
                    itemObject.id,
                    itemName,
                    qty, price))
            addRowToTable(tableItems, itemName, qty, rowTotal,price)
            totalInvoiceAmount += rowTotal
            tvTotalInvoice.text = String.format("%.2f ج.م", totalInvoiceAmount)
            clearInputFields()
        } else {
            Toast.makeText(this, "بيانات الصنف غير مكتملة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveInvoice() {
        if (selectedCustomerId != -1 && tempItemsList.isNotEmpty()) {
            val isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
            val debtAmount = totalInvoiceAmount

            val model = ReturnedModel(
                id = if (isEditMode) currentReturnedId else UUID.randomUUID().toString(),
                customerId = selectedCustomerId,
                returnedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date()),
                userId = Prefs.getUserId(this) ?: "",
                latitude = 0.0,
                longitude = 0.0,
                invoiceNum=etInvoiceNumber.text.toString(),
                updateAt = System.currentTimeMillis(),
            )

            viewModel.saveOrUpdateReturned(isEditMode, model, tempItemsList, debtAmount)
        } else {
            Toast.makeText(this, "يرجى اختيار عميل وأصناف", Toast.LENGTH_SHORT).show()
        }
    }
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    @SuppressLint("DefaultLocale")
    private fun addRowToTable(table: TableLayout, name: String, qty: Int, total: Double, unitPrice: Double) {
        val row = TableRow(this).apply {
            setPadding(0, 4, 0, 4)
        }

        // 1. اسم الصنف
        val tvName = TextView(this).apply {
            text = name; width = dpToPx(150); setPadding(12, 12, 12, 12)
        }

        // 2. خانة الكمية (EditText للتعديل)
        val etQty = EditText(this).apply {
            setText(qty.toString())
            width = dpToPx(70)
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            background = null // لجعلها تبدو كجزء من الجدول
        }

        // 3. خانة سعر الوحدة (EditText للتعديل)
        val etPrice = EditText(this).apply {
            setText(unitPrice.toString())
            width = dpToPx(80)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            gravity = Gravity.CENTER
            background = null
        }

        val tvRowTotal = TextView(this).apply {
            text = String.format("%.2f", total)
            width = dpToPx(90)
            gravity = Gravity.CENTER
        }

        // منطق التحديث التلقائي عند تغيير الكمية أو السعر في الجدول
        val updateTotalWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newQty = etQty.text.toString().toIntOrNull() ?: 0
                val newPrice = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                val newRowTotal = newQty * newPrice

                tvRowTotal.text = String.format("%.2f", newRowTotal)

                // تحديث القائمة البرمجية (tempItemsList)
                val item = tempItemsList.find { it.itemName == name }
                item?.let {
                    it.amount = newQty
                    it.price = newPrice
                }
                recalculateGrandTotal()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etQty.addTextChangedListener(updateTotalWatcher)
        etPrice.addTextChangedListener(updateTotalWatcher)

        // 5. زر الحذف
        val btnDelete = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                table.removeView(row)
                tempItemsList.removeAll { it.itemName == name }
                recalculateGrandTotal()
            }
        }

        row.addView(tvName); row.addView(etQty); row.addView(etPrice); row.addView(tvRowTotal); row.addView(btnDelete)
        table.addView(row)
    }

    // دالة لإعادة حساب إجمالي الفاتورة بالكامل
    @SuppressLint("DefaultLocale")
    private fun recalculateGrandTotal() {
        totalInvoiceAmount = tempItemsList.sumOf { it.amount * it.price }
        tvTotalInvoice.text = String.format("%.2f ج.م", totalInvoiceAmount)
    }

    // عرض رصيد العميل عند الاختيار
    @SuppressLint("DefaultLocale")
    private fun onCustomerSelected(customerModel: CustomerModel) {
        selectedCustomerId = customerModel.id
        val layoutDebt = findViewById<LinearLayout>(R.id.layoutCustomerDebt)
        val tvDebt = findViewById<TextView>(R.id.tvCustomerDebt)

        layoutDebt.visibility = View.VISIBLE
        tvDebt.text = String.format("%.2f ج.م", customerModel.customerDebt) // تأكد إن الـ Model فيه currentDebt
    }
    private fun showHistoryIfPossible() {
        val selectedItemName = autoItem.text.toString()
        val itemObject = currentItems.find { it.itemName == selectedItemName }
        if (selectedCustomerId != -1 && itemObject != null) {
            viewModel.loadItemHistory(selectedCustomerId, itemObject.id)
            showHistoryDialog(itemObject.itemName)
        }
    }

    private fun clearInputFields() {
        autoItem.setText(""); etQuantity.setText(""); etSellingPrice.setText("")
    }
    @SuppressLint("MissingInflatedId")
    private fun showHistoryDialog(itemName: String) {
        // 1. إنشاء الـ BottomSheetDialog وتجهيز الواجهة
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_item_history_bottom_sheet, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvHistoryTitle)
        val container = view.findViewById<LinearLayout>(R.id.historyContainer)

        tvTitle.text = "سجل شراء: $itemName"

        // 2. مراقبة البيانات القادمة من الـ ViewModel
        // استخدمنا collectLatest عشان لو البيانات اتحدثت يعرض آخر حاجة
        lifecycleScope.launch {
            viewModel.itemHistory.collectLatest { historyList ->
                container.removeAllViews() // مسح البيانات القديمة (Loading state)

                if (historyList.isEmpty()) {
                    // عرض رسالة في حالة عدم وجود سجل
                    val emptyTv = TextView(this@AddReturnedActivity).apply {
                        text = "لا توجد عمليات بيع سابقة لهذا العميل لهذا الصنف"
                        gravity = Gravity.CENTER
                        setPadding(0, 50, 0, 50)
                        setTextColor(Color.GRAY)
                    }
                    container.addView(emptyTv)
                } else {
                    // عرض السجل سطر بسطر
                    historyList.forEach { history ->
                        val historyView = layoutInflater.inflate(R.layout.item_history_row, container, false)

                        historyView.findViewById<TextView>(R.id.tvHistoryDate).text = history.date
                        historyView.findViewById<TextView>(R.id.tvHistoryQty).text = "الكمية: ${history.amount}"
                        historyView.findViewById<TextView>(R.id.tvHistoryPrice).text = "السعر: ${history.price} ج.م"

                        // ميزة احترافية: عند الضغط على سطر قديم، يتم وضع السعر في الحقل الرئيسي
                        historyView.setOnClickListener {
                            etSellingPrice.setText(history.price.toString())
                            bottomSheet.dismiss() // إغلاق النافذة بعد الاختيار
                            Toast.makeText(this@AddReturnedActivity, "تم اختيار السعر القديم", Toast.LENGTH_SHORT).show()
                        }

                        container.addView(historyView)
                    }
                }
            }
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()

        // تصفير السجل في الـ ViewModel عند إغلاق الـ Dialog عشان ميعرضش داتا قديمة المرة الجاية
        bottomSheet.setOnDismissListener {
            viewModel.clearHistory()
        }
    }
    // ... دالة showHistoryDialog كما هي عندك ...
}