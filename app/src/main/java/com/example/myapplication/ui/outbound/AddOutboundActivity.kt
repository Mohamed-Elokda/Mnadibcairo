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

@AndroidEntryPoint
class AddOutboundActivity : AppCompatActivity() {
    private lateinit var outboundScrollView: ScrollView 

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Toast.makeText(this, "تم منح صلاحية الموقع الدقيق", Toast.LENGTH_SHORT).show()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Toast.makeText(this, "تم منح صلاحية الموقع التقريبي", Toast.LENGTH_SHORT).show()
            }
            else -> {
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
    private var selectedCustomer: Int? = null 
    private var totalInvoiceAmount = 0.0

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
    private var paidAmount: Double = 0.0
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
            paidAmount = intent.getDoubleExtra("PAID_AMOUNT", 0.0)

            oldAccount.text = previousDebt.toString()
            etReceivedAmount.setText(paidAmount.toString())
            invoceView.setText(invoiceNum)
            autoCustomer.setText(customerName)

            viewModel.loadInvoiceDetails(editOutboundId)

            viewModel.invoiceDetails.observe(this@AddOutboundActivity){
                tempDetailsList.clear()
                tableItems.removeAllViews()
                it.forEach { details->
                    addItemToTable(details.id,details.quantity.toString(),details.price,details.itemId.toLong() ,details.itemName)
                }
            }

            saveButton.text = "تحديث الفاتورة" 
        }

        val mainOutbound = findViewById<ConstraintLayout>(R.id.mainOutbound)
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
        
        saveButton.setOnClickListener {
            saveButton.isEnabled = false
            if (selectedCustomer == null) {
                showAddCustomerConfirmation(autoCustomer.text.toString())
            } else {           
                saveInvoice()
            }
        }

        etReceivedAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                calculateFinalAccount()
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
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
        viewModel.allItems.observe(this) { items ->
            if (items.isNotEmpty() && ::autoItem.isInitialized) {

                val adapter = object : ArrayAdapter<Stock>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    ArrayList(items)
                ) {
                    private val fullList = ArrayList(items) 

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

        viewModel.allCustomers(this).observe(this) { customers ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, customers)
            autoCustomer.setAdapter(adapter)
            autoCustomer.setOnItemClickListener { _, _, position, _ ->
                selectedCustomer = adapter.getItem(position)?.id
                previousDebt = adapter.getItem(position)?.customerDebt ?: 0.0
                oldAccount.text = String.format(Locale.US, "%.2f", previousDebt)
                calculateFinalAccount()
            }
        }

        viewModel.saveResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> saveButton.isEnabled = false
                is Resource.Success -> {
                    Toast.makeText(this, "تم حفظ الفاتورة بنجاح", Toast.LENGTH_LONG).show()
                }
                is Resource.Error -> {
                    saveButton.isEnabled = true
                    Toast.makeText(this, "خطأ: ${resource.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun calculateFinalAccount() {
        val oldDebt = previousDebt
        val currentInvoice = totalInvoiceAmount
        val paid = etReceivedAmount.text.toString().toDoubleOrNull() ?: 0.0
        val finalRemainder = (oldDebt + currentInvoice) - paid

        tvTotal.text = String.format(Locale.US, "%.2f ج.م", currentInvoice)
        newAccount.text = String.format(Locale.US, "%.2f ج.م", finalRemainder)
    }

    private fun addItemToTable(detailsId: String, qtyInput: String, price: Double, itemId: Long, itemName: String) {
        if (itemName.isEmpty() || qtyInput.isEmpty()) {
            etQuantity.error = "اختر صنفاً وكمية صحيحة"
            return
        }

        val result = calculateResult(qtyInput)
        val qtyInt = result.toIntOrNull() ?: 0
        val itemTotal = qtyInt * price

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

        val row = TableRow(this).apply {
            setPadding(0, 8, 0, 8)
            if (tableItems.childCount % 2 == 0) setBackgroundColor(Color.parseColor("#F9F9F9"))
        }

        row.addView(TextView(this).apply {
            text = itemName
            setPadding(8, 8, 8, 8)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.2f)
        })

        val tvRowTotal = TextView(this).apply {
            text = String.format(Locale.US, "%.2f", itemTotal)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.8f)
        }

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
                    tvRowTotal.text = String.format(Locale.US, "%.2f", newQty * detail.price)
                    recalculateGrandTotal()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        val etPriceInRow = EditText(this).apply {
            setText(price.toString())
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = null
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.7f)

            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val newPrice = s.toString().toDoubleOrNull() ?: 0.0
                    detail.price = newPrice
                    tvRowTotal.text = String.format(Locale.US, "%.2f", detail.amount * newPrice)
                    recalculateGrandTotal()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        val btnDelete = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.RED)
            setOnClickListener {
                tableItems.removeView(row)
                tempDetailsList.remove(detail)
                recalculateGrandTotal()
            }
        }

        row.addView(etQtyInRow)
        row.addView(etPriceInRow)
        row.addView(tvRowTotal)
        row.addView(btnDelete)

        tableItems.addView(row)
        recalculateGrandTotal()

        autoItem.setText("")
        etQuantity.setText("")
        etPrice.setText("")
        selectedItem = null
        autoItem.requestFocus()
    }

    private fun recalculateGrandTotal() {
        totalInvoiceAmount = tempDetailsList.sumOf { it.amount * it.price }
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
            .setNegativeButton("إلغاء") { _, _ -> saveButton.isEnabled = true }
            .show()
    }

    private fun showAddCustomerConfirmation(customerName: String) {
        AlertDialog.Builder(this)
            .setTitle("عميل جديد؟")
            .setMessage("العميل \"$customerName\" غير مسجل. هل تريد إضافته كعميل جديد والحفظ؟")
            .setPositiveButton("نعم، أضف واحفظ") { _, _ -> saveInvoice() }
            .setNegativeButton("تعديل الاسم") { dialog, _ ->
                saveButton.isEnabled = true
                autoCustomer.requestFocus()
                dialog.dismiss()
            }.show()
    }

    private fun saveInvoice() {
        if (!hasLocationPermission()) {
            checkAndRequestPermissions()
            saveButton.isEnabled = true
            return
        }
        if (!isLocationEnabled()) {
            showLocationSettingsDialog()
            return
        }

        val customerNameInInput = autoCustomer.text.toString().trim()
        val invoiceNumStr = invoceView.text.toString().trim()

        if (invoiceNumStr.isEmpty()) {
            invoceView.error = "يرجى إدخال رقم الفاتورة"
            saveButton.isEnabled = true
            return
        }

        val invoiceNum = invoiceNumStr.toIntOrNull() ?: 0
        val received = etReceivedAmount.text.toString().toDoubleOrNull() ?: 0.0

        if (customerNameInInput.isEmpty()) {
            autoCustomer.error = "يرجى اختيار العميل"
            saveButton.isEnabled = true
            return
        }

        if (tempDetailsList.isEmpty()) {
            Toast.makeText(this, "أضف صنفاً واحداً على الأقل", Toast.LENGTH_SHORT).show()
            saveButton.isEnabled = true
            return
        }

        lifecycleScope.launch {
            try {
                Toast.makeText(this@AddOutboundActivity, "جاري تحديد الموقع وحفظ الفاتورة...", Toast.LENGTH_SHORT).show()

                val locationHelper = LocationHelper(this@AddOutboundActivity)
                val coords = locationHelper.getCurrentLocation()

                if (coords == null) {
                    Toast.makeText(this@AddOutboundActivity, "فشل تحديد الموقع!", Toast.LENGTH_LONG).show()
                    saveButton.isEnabled = true
                    return@launch
                }

                val finalCustomerId = selectedCustomer ?: viewModel.addCustomerQuickly(customerNameInInput, this@AddOutboundActivity)

                val outbound = Outbound(
                    id = editOutboundId,
                    userId = Prefs.getUserId(this@AddOutboundActivity) ?: "",
                    customerId = finalCustomerId,
                    invorseNumber = invoiceNum,
                    image = "",
                    outboundDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date()),
                    moneyResive = received, // تم الإصلاح: لم يعد يحول لـ Int
                    previousDebt = previousDebt,
                    totalRemainder = (previousDebt + totalInvoiceAmount) - received,
                    isSynced = false,
                    customerName = customerNameInInput,
                    latitude = coords.first,
                    longitude = coords.second,
                    updatedAt = System.currentTimeMillis()
                )

                if (isEditMode) {
                    viewModel.updateInvoice(outbound, tempDetailsList)
                } else {
                    viewModel.saveInvoice(outbound, tempDetailsList)
                }
                
                scheduleSync(this@AddOutboundActivity)
                showPrintConfirmation(outbound, tempDetailsList)

            } catch (e: Exception) {
                saveButton.isEnabled = true
                Toast.makeText(this@AddOutboundActivity, "خطأ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPrintConfirmation(outbound: Outbound, details: List<OutboundDetails>) {
        AlertDialog.Builder(this)
            .setTitle("نجاح العملية")
            .setMessage("تم حفظ الفاتورة بنجاح. هل تريد طباعتها الآن؟")
            .setPositiveButton("طباعة") { _, _ -> /* الطباعة */ }
            .setNegativeButton("إغلاق") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun initViews() {
        autoCustomer = findViewById(R.id.autoCustomer)
        autoItem = findViewById(R.id.autoItem)
        etQuantity = findViewById(R.id.etQuantity)
        invoceView = findViewById(R.id.etInvoiceNumber)
        saveButton = findViewById(R.id.btnSave)
        etPrice = findViewById(R.id.etSellingPrice)
        etReceivedAmount = findViewById(R.id.etPaidAmount)
        tvTotal = findViewById(R.id.tvTotalInvoice)
        oldAccount = findViewById(R.id.oldAccount)
        newAccount = findViewById(R.id.newAccount)
        tableItems = findViewById(R.id.tableItems)
        outboundScrollView = findViewById(R.id.outboundScrollView)
    }
}