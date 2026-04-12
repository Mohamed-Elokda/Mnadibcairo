package com.example.myapplication.ui.outbound

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.core.LocationHelper
import com.example.myapplication.core.calculateResult
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.OutboundRepoImpl
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.Resource
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.useCase.AddOutboundUseCase
import com.example.myapplication.domin.useCase.FetchRemoteOutboundsUseCase
import com.example.myapplication.domin.useCase.ProcessOutboundUseCase
import com.example.myapplication.ui.factory.OutboundViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.isNotEmpty

class AddOutboundActivity : AppCompatActivity() {
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
    private val viewModel: OutboundViewModel by viewModels {
        val db = AppDatabase.getDatabase(this)

        // 1. إنشاء الـ Repositories المطلوبة
        val outboundRepo = OutboundRepoImpl(
            db.outboundDao(),
            db.outboundDetailesDao(),
            db.stockDao(),
            db.itemsDao(),
            db.customerDao()
        )
        val customerRepo = CustomerRepoImpl(db.customerDao()) // الـ Repo الناقص
        val fetchRemoteUseCase =FetchRemoteOutboundsUseCase(outboundRepo)

        // 2. تمرير كل المتطلبات للمصنع
        OutboundViewModelFactory(
            fetchRemoteUseCase,
            ProcessOutboundUseCase(outboundRepo),
            outboundRepo,
            customerRepo // تم تمريره هنا الآن
        )
    }

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_outbound)

        initViews()
        setupObservers()
        checkAndRequestPermissions()

        findViewById<Button>(R.id.btnAddItem).setOnClickListener { addItemToTable() }
        saveButton = findViewById(R.id.btnSave)
        saveButton.setOnClickListener {
            saveButton.isEnabled = false
            saveInvoice()
        }

        etReceivedAmount.addTextChangedListener(object : TextWatcher{
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
                newAccount.text = (old + totalInvoiceAmount - received).toString()            }

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
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
            autoItem.setAdapter(adapter)
            autoItem.setOnItemClickListener { parent, _, position, _ ->
                selectedItem = parent.getItemAtPosition(position) as Stock

            }
        }

        // مراقبة العملاء
        viewModel.allCustomers(this).observe(this) { customers ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, customers)
            autoCustomer.setAdapter(adapter)
            autoCustomer.setOnItemClickListener { _, _, position, _ ->
                selectedCustomer = adapter.getItem(position)?.id
                oldAccount.text=adapter.getItem(position)?.customerDebt.toString()
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
                    finish() // العودة للشاشة السابقة
                }

                is Resource.Error -> {
                    Toast.makeText(this, "خطأ: ${resource.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun addItemToTable() {
        val qty = etQuantity.text.toString()
        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val item = selectedItem



        var result="0"
        if (qty.isNotEmpty()) {
            result = calculateResult(qty)
            etQuantity.setText(result)
            // لنقل المؤشر لآخر الرقم
            etQuantity.setSelection(etQuantity.text.length)
        }else{
            etQuantity.error = "اختر صنفاً وكمية صحيح"
            return
        }

        val itemTotal = result.toInt() * price

        // إنشاء كائن التفاصيل
        val detail = OutboundDetails(
            itemId = item?.ItemId!!,
            amount = result.toInt(),
            price = price,
            outboundId = 0,
            isSynced = false
        )

        // إضافة للقائمة المؤقتة
        tempDetailsList.add(detail)
// داخل onTextChanged

        // إضافة صف للجدول UI
        val row = TableRow(this).apply {
            setPadding(0, 8, 0, 8)
        }

        row.addView(TextView(this).apply { text = item.itemName; setPadding(8, 8, 8, 8); layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f) })
        row.addView(TextView(this).apply { text = result; gravity = android.view.Gravity.CENTER; setPadding(8, 8, 8, 8) })
        row.addView(TextView(this).apply { text = price.toString(); gravity = android.view.Gravity.CENTER; setPadding(8, 8, 8, 8) })

        row.addView(TextView(this).apply { text = itemTotal.toString(); gravity = android.view.Gravity.CENTER; setPadding(8, 8, 8, 8) })

        // إضافة زر الحذف
        val btnDelete = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete) // أيقونة الحذف الافتراضية
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnClickListener {
                // 1. إزالة من الجدول (UI)
                tableItems.removeView(row)
                // 2. إزالة من القائمة البرمجية
                tempDetailsList.remove(detail)
                // 3. تحديث الإجمالي
                totalInvoiceAmount -= itemTotal
                val received = etReceivedAmount.text.toString().toDoubleOrNull() ?: 0.0
                val old = oldAccount.text.toString().toDoubleOrNull() ?: 0.0
                newAccount.text = (old + totalInvoiceAmount - received).toString()
                updateUI()
            }
        }
        row.addView(btnDelete)

        tableItems.addView(row)

        totalInvoiceAmount += itemTotal
        updateUI()
        val received = etReceivedAmount.text.toString().toDoubleOrNull() ?: 0.0
        val old = oldAccount.text.toString().toDoubleOrNull() ?: 0.0
        newAccount.text = (old + totalInvoiceAmount - received).toString()
        // مسح الحقول للإدخال التالي
        autoItem.setText("")
        etQuantity.setText("")
        etPrice.setText("")
        selectedItem = null
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
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

    private fun saveInvoice() {
        if (!hasLocationPermission()) {
            checkAndRequestPermissions() // اطلبها إذا لم تكن موجودة
            return
        }
        if (!isLocationEnabled()) {
            showLocationSettingsDialog()
            return
        }
        val received = etReceivedAmount.text.toString().toIntOrNull() ?: 0
        val customerNameInInput = autoCustomer.text.toString().trim()
        val invoceNum: Int = invoceView.text.toString().trim().toInt()

        if (customerNameInInput.isEmpty() || tempDetailsList.isEmpty()) {
            Toast.makeText(this, "بيانات ناقصة! يرجى إدخال اسم العميل والأصناف", Toast.LENGTH_SHORT)
                .show()
            return
        }

        lifecycleScope.launch {
            // 1. التحقق: هل العميل جديد أم مختار من القائمة؟
            val finalCustomerId = if (selectedCustomer == null) {
                // إضافة العميل الجديد والحصول على الـ ID
                viewModel.addCustomerQuickly(customerNameInInput,this@AddOutboundActivity)
            } else {
                selectedCustomer!!
            }
            val locationHelper = LocationHelper(this@AddOutboundActivity)

            // 1. جلب الإحداثيات
            val coords = locationHelper.getCurrentLocation()
            if (coords == null) {
                Toast.makeText(
                    this@AddOutboundActivity,
                    "فشل تحديد الموقع! تأكد من تفعيل الـ GPS والاتصال بالإنترنت أولاً.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch // الخروج من الكورتين وعدم تنفيذ الحفظ
            }
            val lat = coords?.first   // خط العرض
            val lon = coords?.second  // خط الطول
            // 2. إنشاء كائن الفاتورة بالـ ID النهائي
            val outbound = Outbound(
                id = 0,
                userId = Prefs.getUserId(this@AddOutboundActivity)?:"",
                customerId = finalCustomerId,
                invorseNumber = invoceNum,
                image = "",
                outboundDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                moneyResive = received,
                isSynced = false,
                customerName = "",
                latitude = lat!!,
                longitude = lon!!
            )

            // 3. حفظ الفاتورة
            viewModel.saveInvoice(outbound, tempDetailsList)
            scheduleSync(this@AddOutboundActivity)
        }
    }


    private fun updateUI() {
        tvTotal.text = "الإجمالي: $totalInvoiceAmount ج.م"
    }

    private fun initViews() {
        autoCustomer = findViewById(R.id.autoCustomer)
        autoItem = findViewById(R.id.autoItem)
        etQuantity = findViewById(R.id.etQuantity)
        invoceView = findViewById(R.id.etInvoiceNumber)

        // الأسطر الناقصة التي يجب إضافتها:
        etPrice = findViewById(R.id.etSellingPrice)
        etReceivedAmount = findViewById(R.id.etPaidAmount)

        tvTotal = findViewById(R.id.tvTotalInvoice)
        oldAccount = findViewById(R.id.oldAccount)
        newAccount = findViewById(R.id.newAccount)
        tableItems = findViewById(R.id.tableItems)
    }
}