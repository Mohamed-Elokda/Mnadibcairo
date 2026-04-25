package com.example.myapplication.ui.outbound

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.myapplication.R
import com.example.myapplication.core.LocationHelper
import com.example.myapplication.core.calculateResult
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.model.Resource
import com.example.myapplication.domin.model.Stock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.text.toLong

@AndroidEntryPoint
class AddOutboundActivity : AppCompatActivity() {
    private lateinit var outboundScrollView: ScrollView // تعريف المتغير

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // تم منح صلاحية الموقع الدقيق - يمكنك جلب اللوكيشن الآن
                Toast.makeText(this, "تم منح صلاحية الموقع الدقيق", Toast.LENGTH_SHORT).show()
            }

            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // تم منح صلاحية الموقع التقريبي فقط
                Toast.makeText(this, "تم منح صلاحية الموقع التقريبي", Toast.LENGTH_SHORT).show()
            }

            else -> {
                // المستخدم رفض الصلاحيات
                Toast.makeText(
                    this,
                    "لا يمكن استخدام البرنامج بدون صلاحية الموقع",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private val tempDetailsList = mutableListOf<OutboundDetails>()
    private var selectedItem: Stock? = null
    private var selectedCustomer: Int? = null // سنخزن ID العميل
    private var totalInvoiceAmount = 0.0

    // تهيئة الـ ViewModel الخاص بالصادر
    private val viewModel: OutboundViewModel by viewModels()
    private lateinit var autoCustomer: AutoCompleteTextView
    private lateinit var autoItem: AutoCompleteTextView
    private lateinit var etQuantity: EditText
    private lateinit var etPrice: EditText
    private lateinit var invoceView: EditText
    private lateinit var etReceivedAmount: EditText
    private lateinit var tvTotal: TextView
    private lateinit var oldAccount: TextView
    private lateinit var newAccount: TextView
    private lateinit var tableItems: TableLayout
    private lateinit var saveButton: Button
    private var isEditMode = false
    private lateinit var invoiceNum: String
    private lateinit var customerName: String
    private lateinit var date: String
    private var previousDebt: Double = 0.0
    private var PAID_AMOUNT: Int = 0
    private var totalRemainder: Double = 0.0
    private var editOutboundId: String =""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_outbound)

        initViews()
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        if (isEditMode) {
            editOutboundId = intent.getStringExtra("OUTBOUND_ID").toString()
            selectedCustomer = intent.getIntExtra("customerId",-1)
            invoiceNum = intent.getStringExtra("INVOICE_NUM") ?: ""
            customerName = intent.getStringExtra("CUSTOMER_NAME") ?: ""
            date = intent.getStringExtra("DATE") ?: ""
            previousDebt = intent.getDoubleExtra("previousDebt", 0.0)
            totalRemainder = intent.getDoubleExtra("totalRemainder", 0.0)
            PAID_AMOUNT = intent.getIntExtra("PAID_AMOUNT", 0)

            oldAccount.setText(previousDebt.toString())
            etReceivedAmount.setText(PAID_AMOUNT.toString())
            invoceView.setText(invoiceNum)
            autoCustomer.setText(customerName)

            viewModel.loadInvoiceDetails(editOutboundId)

            viewModel.invoiceDetails.observe(this@AddOutboundActivity){
                tempDetailsList.clear()
                tableItems.removeAllViews()
                it.forEach { details->
                    addItemToTable(details.id,details.quantity.toString(),details.price,details.itemId ,details.itemName)


                }
            }

            saveButton.text = "تحديث الفاتورة" // تغيير نص الزرار


        }

        val mainOutbound = findViewById<ConstraintLayout>(R.id.mainOutbound)
        val outboundScrollView = findViewById<ScrollView>(R.id.outboundScrollView)

// حل مشكلة الـ StatusBar
        ViewCompat.setOnApplyWindowInsetsListener(mainOutbound) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupObservers()
        checkAndRequestPermissions()

        findViewById<Button>(R.id.btnAddItem).setOnClickListener {
            val qtyInput = etQuantity.text.toString()
            val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
            val item = selectedItem

            addItemToTable("",qtyInput,price,item?.ItemId?.toLong()?:0,item?.itemName?:"") }
        saveButton = findViewById(R.id.btnSave)
        saveButton.setOnClickListener {
            saveButton.isEnabled = false
            if (selectedCustomer == null) {
                showAddCustomerConfirmation(autoCustomer.text.toString())

            } else {           saveInvoice()}
        }

        etReceivedAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {
            }

            override fun onTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {
// داخل onTextChanged
                val received = p0.toString().toDoubleOrNull() ?: 0.0
                val old = oldAccount.text.toString().toDoubleOrNull() ?: 0.0
                newAccount.text = (old + totalInvoiceAmount - received).toString()
            }

        })
        // استقبال بيانات الذكاء الاصطناعي (Gemini)
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun checkAndRequestPermissions() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun setupObservers() {
        // مراقبة الأصناف
        viewModel.allItems.observe(this) { items ->
            if (items.isNotEmpty() && ::autoItem.isInitialized) {

                val adapter = object : ArrayAdapter<Stock>(
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
                                    addAll(results.values as List<Stock>)
                                }
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
                autoItem.setAdapter(adapter)
                autoItem.setOnClickListener { autoItem.showDropDown() }
                autoItem.setOnItemClickListener { parent, _, position, _ ->
                    val selected = parent.getItemAtPosition(position) as Stock
                    selectedItem= selected
                }
            }
        }


        // مراقبة العملاء
        viewModel.allCustomers(this).observe(this) { customers ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, customers)
            autoCustomer.setAdapter(adapter)
            autoCustomer.setOnItemClickListener { _, _, position, _ ->
                selectedCustomer = adapter.getItem(position)?.id
                oldAccount.text = adapter.getItem(position)?.customerDebt.toString()
            }
        }

        // مراقبة نتيجة الحفظ
        viewModel.saveResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    saveButton.isEnabled = false
                }

                is Resource.Success -> {
                    Toast.makeText(this, "تم حفظ الفاتورة بنجاح", Toast.LENGTH_LONG).show()
                }

                is Resource.Error -> {
                    Toast.makeText(this, "خطأ: ${resource.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun calculateFinalAccount() {
        // 1. جلب الرصيد السابق من التكست فيو (المخزن عند اختيار العميل)
        val oldDebt = oldAccount.text.toString().replace(" ج.م", "").toDoubleOrNull() ?: 0.0

        // 2. إجمالي الفاتورة الحالية (المتغير اللي بنحدثه عند إضافة أصناف)
        val currentInvoice = totalInvoiceAmount

        // 3. المبلغ المدفوع من الإيديت تيكست
        val paid = etReceivedAmount.text.toString().toDoubleOrNull() ?: 0.0

        // 4. الحسبة النهائية
        val finalRemainder = (oldDebt + currentInvoice) - paid

        // 5. تحديث الواجهة
        tvTotal.text = String.format("%.2f ج.م", currentInvoice)
        newAccount.text = String.format("%.2f ج.م", finalRemainder)
    }

    private fun addItemToTable(detailsId: String, qtyInput: String, price: Double, itemId: Long, itemName: String) {


        // 1. التحقق من البيانات
        if (itemName.isEmpty() || qtyInput.isEmpty()) {
            etQuantity.error = "اختر صنفاً وكمية صحيحة"
            return
        }

        val result = calculateResult(qtyInput)
        val qtyInt = result.toIntOrNull() ?: 0
        val itemTotal = qtyInt * price

        // 2. إنشاء كائن التفاصيل وإضافته للقائمة
        val detail = OutboundDetails(
            itemId = itemId.toInt(),
            amount = qtyInt,
            price = price,
            outboundId = 0,
            isSynced = false,
            id = detailsId,
            updatedAt = System.currentTimeMillis()
        )
        tempDetailsList.add(detail)

        // 3. إنشاء صف الجدول (UI)
        val row = TableRow(this).apply {
            setPadding(0, 8, 0, 8)
            // إضافة لون خلفية خفيف للصفوف الزوجية لسهولة القراءة
            if (tableItems.childCount % 2 == 0) setBackgroundColor(
                Color.parseColor(
                    "#F9F9F9"
                )
            )
        }

        // أ - اسم الصنف
        row.addView(TextView(this).apply {
            text = itemName
            setPadding(8, 8, 8, 8)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.2f)
        })

        // ب - إجمالي الصف (سنعرفه هنا ليتم استخدامه داخل الـ TextWatchers)
        val tvRowTotal = TextView(this).apply {
            text = String.format("%.2f", itemTotal)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.8f)
        }

        // ج - حقل الكمية (Editable)
        val etQtyInRow = EditText(this).apply {
            setText(qtyInt.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
            background = null
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.6f)

            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val newQty = s.toString().toIntOrNull() ?: 0
                    detail.amount = newQty
                    // تحديث إجمالي الصف فوراً
                    val currentPrice = detail.price
                    tvRowTotal.text = String.format("%.2f", newQty * currentPrice)
                    recalculateGrandTotal()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    outboundScrollView.postDelayed({
                        outboundScrollView.smoothScrollTo(
                            0,
                            row.top
                        )
                    }, 300)
                }
            }
        }

        // د - حقل السعر (Editable)
        val etPriceInRow = EditText(this).apply {
            setText(price.toString())
            inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = null
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.7f)

            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val newPrice = s.toString().toDoubleOrNull() ?: 0.0
                    detail.price = newPrice
                    // تحديث إجمالي الصف فوراً
                    val currentQty = detail.amount
                    tvRowTotal.text = String.format("%.2f", currentQty * newPrice)
                    recalculateGrandTotal()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        // هـ - زر الحذف
        val btnDelete = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.RED)
            setOnClickListener {
                tableItems.removeView(row)
                tempDetailsList.remove(detail)
                recalculateGrandTotal() // إعادة الحساب بعد الحذف
            }
        }

        // 4. إضافة العناصر للصف ثم إضافة الصف للجدول
        row.addView(etQtyInRow)
        row.addView(etPriceInRow)
        row.addView(tvRowTotal)
        row.addView(btnDelete)

        tableItems.addView(row)

        // 5. تحديث الحسابات النهائية وتصفير الحقول العلوية
        recalculateGrandTotal()

        autoItem.setText("")
        etQuantity.setText("")
        etPrice.setText("")
        selectedItem = null
        autoItem.requestFocus() // إعادة التركيز لاختيار الصنف التالي
    }


    private fun recalculateGrandTotal() {
        // حساب الإجمالي من القائمة الفعلية لضمان الدقة
        totalInvoiceAmount = tempDetailsList.sumOf { it.amount * it.price }

        // تحديث النص الخاص بإجمالي الفاتورة
        tvTotal.text = String.format("%.2f ج.م", totalInvoiceAmount)

        // استدعاء دالة حساب المتبقي (التي تعتمد على الرصيد القديم والمدفوع)
        calculateFinalAccount()
    }


    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("نظام التتبع والموقع")
            .setMessage("لا يمكن إصدار فاتورة بدون تحديد الموقع. يرجى تفعيل الـ GPS للمتابعة.")
            .setPositiveButton("تفعيل") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
    private fun showAddCustomerConfirmation(customerName: String) {
        AlertDialog.Builder(this)
            .setTitle("عميل جديد؟")
            .setMessage("العميل \"$customerName\" غير مسجل. هل تريد إضافته كعميل جديد والحفظ؟")
            .setPositiveButton("نعم، أضف واحفظ", DialogInterface.OnClickListener { _, _ ->
                // كود الحفظ هنا
                saveInvoice()
            })
            .setNegativeButton("تعديل الاسم") { dialog, _ ->
                // كود التعديل هنا
                saveButton.isEnabled = true
                autoCustomer.requestFocus()
                dialog.dismiss()
            }.show()
    }
    private fun saveInvoice() {
        // 1. الأمان الجغرافي
        if (!hasLocationPermission()) {
            checkAndRequestPermissions()
            return
        }
        if (!isLocationEnabled()) {
            showLocationSettingsDialog()
            return
        }

        // 2. التحقق من المدخلات الأساسية (Validation)
        val customerNameInInput = autoCustomer.text.toString().trim()
        val invoiceNumStr = invoceView.text.toString().trim()

        if (invoiceNumStr.isEmpty()) {
            invoceView.error = "يرجى إدخال رقم الفاتورة"
            saveButton.isEnabled = true // تعطيل الزر فوراً

            return
        }

        val invoiceNum = invoiceNumStr.toIntOrNull() ?: 0
        val received = etReceivedAmount.text.toString().toDoubleOrNull() ?: 0.0

        if (customerNameInInput.isEmpty()) {
            autoCustomer.error = "يرجى اختيار العميل"
            saveButton.isEnabled = true // تعطيل الزر فوراً

            return
        }

        if (tempDetailsList.isEmpty()) {
            Toast.makeText(
                this,
                "لا يمكن حفظ فاتورة فارغة! أضف صنفاً واحداً على الأقل",
                Toast.LENGTH_SHORT
            ).show()
            saveButton.isEnabled = true // تعطيل الزر فوراً

            return
        }

        // 3. منع تكرار الحفظ (Debounce)

        lifecycleScope.launch {
            try {
                saveButton.isEnabled = false // تعطيل الزر فوراً

                // إظهار رسالة جاري الحفظ
                Toast.makeText(
                    this@AddOutboundActivity,
                    "جاري تحديد الموقع وحفظ الفاتورة...",
                    Toast.LENGTH_SHORT
                ).show()

                // جلب الموقع
                val locationHelper = LocationHelper(this@AddOutboundActivity)
                val coords = locationHelper.getCurrentLocation()

                if (coords == null) {
                    Toast.makeText(
                        this@AddOutboundActivity,
                        "فشل تحديد الموقع! حاول مرة أخرى",
                        Toast.LENGTH_LONG
                    ).show()
                    saveButton.isEnabled = true // إعادة تفعيل الزر للمحاولة مرة أخرى
                    return@launch
                }

                val lat = coords.first
                val lon = coords.second

                // جلب الـ Customer ID (جديد أو قديم)
                val finalCustomerId = selectedCustomer ?: viewModel.addCustomerQuickly(
                    customerNameInInput,
                    this@AddOutboundActivity
                )


                // 4. بناء الكائن بأمان
                val outbound = Outbound(
                    id = editOutboundId,
                    userId = Prefs.getUserId(this@AddOutboundActivity) ?: "",
                    customerId = finalCustomerId,
                    invorseNumber = invoiceNum,
                    image = "",
                    outboundDate = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    ).format(Date()),
                    moneyResive = received.toInt(),
                    previousDebt = oldAccount.text.toString().replace(" ج.م", "").toDoubleOrNull()
                        ?: 0.0,
                    totalRemainder = newAccount.text.toString().replace(" ج.م", "").toDoubleOrNull()
                        ?: 0.0,
                    isSynced = false,
                    customerName = customerNameInInput,
                    latitude = lat,
                    longitude = lon,
                    updatedAt = System.currentTimeMillis()
                )

                // 5. الحفظ النهائي مع معالجة الأخطاء
                if (isEditMode) {
                    viewModel.updateInvoice(outbound, tempDetailsList) // الدالة الجديدة
                } else {
                    viewModel.saveInvoice(outbound, tempDetailsList)
                }
                // نجاح العملية
                scheduleSync(this@AddOutboundActivity)
                Toast.makeText(this@AddOutboundActivity, "تم الحفظ بنجاح", Toast.LENGTH_SHORT)
                    .show()
                showPrintConfirmation(outbound, tempDetailsList)


            } catch (e: Exception) {
                // في حالة حدوث أي خطأ غير متوقع (مثلاً مشكلة في الـ Database)
                saveButton.isEnabled = true
                Toast.makeText(
                    this@AddOutboundActivity,
                    "حدث خطأ غير متوقع: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun showPrintConfirmation(outbound: Outbound, details: List<OutboundDetails>) {
        AlertDialog.Builder(this)
            .setTitle("نجاح العملية")
            .setMessage("تم حفظ الفاتورة بنجاح. هل تريد طباعتها الآن؟")
            .setPositiveButton("طباعة") { _, _ ->
//                generateThermalInvoice(outbound, details)
            }
            .setNegativeButton("إغلاق") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

//    private fun generateThermalInvoice(outbound: Outbound, details: List<OutboundDetails>) {
//        val printerText = StringBuilder()
//
//        // 1. رأس الفاتورة
//        printerText.append("[C]<b><font size='big'>شركة مناديب كارو</font></b>\n")
//        printerText.append("[C]للخدمات اللوجستية والتوزيع\n")
//        printerText.append("[C]--------------------------------\n")
//
//        // 2. بيانات الفاتورة
//        printerText.append("[L]رقم الفاتورة: ${outbound.invorseNumber}\n")
//        printerText.append("[L]العميل: ${outbound.customerName}\n")
//        printerText.append("[L]التاريخ: ${outbound.outboundDate}\n")
//        printerText.append("[C]--------------------------------\n")
//
//        // 3. جدول الأصناف (تم إصلاح جلب الاسم هنا)
//        printerText.append("[L]الصنف          | الكمية | السعر\n")
//        printerText.append("[C]--------------------------------\n")
//
//        details.forEach { item ->
//            // نستخدم حقل itemName الموجود في موديل التفاصيل الخاص بك
//            val name = if (item.itemName.length > 15) item.itemName.substring(0, 12) + ".." else item.itemName
//            printerText.append("[L]${name.padEnd(16)} | ${item.amount.toString().padEnd(5)} | ${item.price}\n")
//        }
//
//        printerText.append("[C]--------------------------------\n")
//
//        // 4. الحسابات
//        printerText.append("[R]إجمالي الفاتورة: ${totalInvoiceAmount} ج.م\n")
//        printerText.append("[R]المدفوع: ${outbound.moneyResive} ج.م\n")
//        printerText.append("[R]<b>المتبقي: ${outbound.totalRemainder} ج.م</b>\n")
//
//        // 5. التذييل
//        printerText.append("[C]--------------------------------\n")
//        printerText.append("[C]رقم الشكاوي: 0123456789\n")
//        printerText.append("[C]شكراً لتعاملكم معنا\n")
//        printerText.append("\n\n")
//
//        // نمرر النص المجهز لدالة اختيار الطابعة
//        openPrinterPicker(printerText.toString())
//    }



    private fun updateUI() {
        tvTotal.text = "الإجمالي: $totalInvoiceAmount ج.م"
    }





    private fun sendToPrinter(printContent: String) {
        // 1. البحث عن طابعة متصلة عبر البلوتوث
        val connection = BluetoothPrintersConnections.selectFirstPaired()

        if (connection != null) {
            try {
                // 2. إنشاء كائن الطابعة (32 هو عرض الورقة لـ 58mm، استخدم 48 لـ 80mm)
                val printer = EscPosPrinter(connection, 203, 48f, 32)

                // 3. أمر الطباعة مع دعم العربية واللوجو
                // [C] تعني Center, [L] Left, [R] Right
                // <img> تُستخدم لطباعة اللوجو (يجب أن يكون بصيغة Hexadecimal أو Bitmap)

                printer.printFormattedTextAndCut(printContent)

                Toast.makeText(this, "جاري الطباعة...", Toast.LENGTH_SHORT).show()
                finish() // إغلاق الشاشة بعد النجاح

            } catch (e: Exception) {
                Log.e("Printer", "خطأ في الطباعة: ${e.message}")
                Toast.makeText(this, "فشل الاتصال بالطابعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "لم يتم العثور على طابعة بلوتوث مقترنة!", Toast.LENGTH_LONG).show()
            // فتح إعدادات البلوتوث للمستخدم
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
    }
    // دالة لجلب البيانات في وضع التعديل

    private fun initViews() {
        autoCustomer = findViewById(R.id.autoCustomer)
        autoItem = findViewById(R.id.autoItem)
        etQuantity = findViewById(R.id.etQuantity)
        invoceView = findViewById(R.id.etInvoiceNumber)
        saveButton = findViewById(R.id.btnSave)

        // الأسطر الناقصة التي يجب إضافتها:
        etPrice = findViewById(R.id.etSellingPrice)
        etReceivedAmount = findViewById(R.id.etPaidAmount)

        tvTotal = findViewById(R.id.tvTotalInvoice)
        oldAccount = findViewById(R.id.oldAccount)
        newAccount = findViewById(R.id.newAccount)
        tableItems = findViewById(R.id.tableItems)
        outboundScrollView = findViewById(R.id.outboundScrollView)
    }
}