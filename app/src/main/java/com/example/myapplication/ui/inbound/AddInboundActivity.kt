package com.example.myapplication.ui.inbound

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.core.calculateResult
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.local.entity.InboundDetailWithItemName
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.domin.model.Inbound
import com.example.myapplication.domin.model.InboundDetails
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.SuppliedModel
import com.example.myapplication.domin.useCase.AddInboundUseCase
import com.example.myapplication.domin.useCase.GetInboundDetailsUseCase
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddInboundActivity : AppCompatActivity() {

    // القوائم والبيانات
    private val tempItemsList = mutableListOf<InboundDetails>()
    private var selectedItem: Items? = null

    // تعريف العناصر
    private lateinit var etInvoiceNumber: EditText
    private lateinit var autoItem: AutoCompleteTextView
    private lateinit var etQuantity: EditText
    private lateinit var tableItems: TableLayout

    // عناصر المخازن (Supplied)
    private lateinit var autoFromSupplied: AutoCompleteTextView

    // متغيرات التحكم
    private var isEditMode = false
    private var selectedFromSuppliedId: Int = -1
    private var selectedToSuppliedId: Int = -1
    private var editInboundId: Long = -1
    private var originalDate: String? = null
    private var isDataLoaded = false

    private val viewModel: InboundViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repo = InboundRepositoryImpl(
            database.inboundDao(),
            database.inboundDetailesDao(),
            database.stockDao(),
            database.suppliedDao(),
            itemsDao = database.itemsDao()
        )
        val getInboundDetailsUseCase = GetInboundDetailsUseCase(repo)
        InboundViewModelFactory(AddInboundUseCase(repo), getInboundDetailsUseCase, repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_inbound)

        // 1. ربط العناصر بالـ XML أولاً
        initViews()

        // 2. تصفية البيانات القديمة
        tempItemsList.clear()

        // 3. إعداد المراقبين (Observers)
        setupObservers()

        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        if (isEditMode) {
            editInboundId = intent.getLongExtra("INBOUND_ID", -1)
            originalDate = intent.getStringExtra("DATE")
            setupEditMode()
        }

        findViewById<Button>(R.id.btnAddItem).setOnClickListener { checkAndAddItem() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveFullInvoice() }

        intent.getStringExtra("AI_ITEMS_JSON")?.let { fillTableFromAI(it) }
    }

    private fun initViews() {
        etInvoiceNumber = findViewById(R.id.etInvoiceNumber)
        autoItem = findViewById(R.id.autoItem)
        etQuantity = findViewById(R.id.etQuantity)
        tableItems = findViewById(R.id.tableItems)



        // ربط الـ AutoComplete الخاص بالمخازن
        // تأكد أن هذه الـ IDs موجودة فعلياً في ملف activity_add_inbound.xml
        autoFromSupplied = findViewById(R.id.fromAutoSupplier)
        // في initViews أو onCreate
        autoItem.threshold = 1

        autoItem.setOnClickListener {
            autoItem.showDropDown()
        }

        autoItem.addTextChangedListener { text ->
            if (text.isNullOrEmpty()) {
                autoItem.post { autoItem.showDropDown() }
            }
        }

// مهم جداً: حفظ العنصر المختار
        autoItem.setOnItemClickListener { parent, _, position, _ ->
            selectedItem = parent.getItemAtPosition(position) as Items
        }
    }

    private fun setupObservers() {
        // مراقبة الأصناف
        // داخل setupObservers
        viewModel.allItems.observe(this) { items ->
            if (items.isNotEmpty() && ::autoItem.isInitialized) {

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

        // مراقبة المخازن (Supplied) وملء القوائم
        // داخل setupObservers في AddInboundActivity
        viewModel.allSupplied.observe(this) { suppliedList ->
            Log.d("DEBUG_DATA", "Supplied List Size: ${suppliedList.size}")

            if (suppliedList.isNotEmpty() && ::autoFromSupplied.isInitialized) {
                // إنشاء Adapter جديد
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suppliedList)

                // ربطه بالمخزن المصدر
                autoFromSupplied.setAdapter(adapter)
                autoFromSupplied.setOnClickListener { autoFromSupplied.showDropDown() }
                autoFromSupplied.setOnItemClickListener { parent, _, position, _ ->
                    val selected = parent.getItemAtPosition(position) as SuppliedModel
                    selectedFromSuppliedId = selected.id
                }

                // ربطه بمخزن الوجهة (Customer/ToSupplied)

            }
        }
    }

    private fun saveFullInvoice() {
        if (tempItemsList.isEmpty()) {
            Toast.makeText(this, "الفاتورة فارغة!", Toast.LENGTH_SHORT).show()
            return
        }

        val invoiceNumStr = etInvoiceNumber.text.toString()
        if (invoiceNumStr.isEmpty()) {
            etInvoiceNumber.error = "مطلوب"
            return
        }

        // التحقق من اختيار المخازن
        if (selectedFromSuppliedId == -1 ) {
            Toast.makeText(this, "يرجى اختيار المخزن  أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        val inbound = Inbound(
            id = if (isEditMode) editInboundId.toInt() else 0,
            userId = Prefs.getUserId(this) ?: "",
            fromSppliedId = selectedFromSuppliedId,
            image = "",
            inboundDate = if (isEditMode) originalDate ?: getCurrentDate() else getCurrentDate(),
            isSynced = false,
            invorseNum = invoiceNumStr.toInt(),
            latitude = 0.0,
            longitude = 0.0,
            suppliedName = ""
        )

        lifecycleScope.launch {
            try {
                viewModel.saveInvoice(inbound, tempItemsList)
                Toast.makeText(this@AddInboundActivity, "تم الحفظ بنجاح!", Toast.LENGTH_SHORT).show()
                scheduleSync(this@AddInboundActivity)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddInboundActivity, "فشل: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupEditMode() {
        findViewById<Button>(R.id.btnSave).text = "تحديث الفاتورة"
        etInvoiceNumber.setText(intent.getStringExtra("INVOICE_NUM"))

        // استقبال الـ ID لضمان عدم ظهور خطأ "يرجى اختيار المخزن"
        selectedFromSuppliedId = intent.getIntExtra("SUPPLIED_ID", -1)
        autoFromSupplied.setText(intent.getStringExtra("SUPPLIER"), false)

        val detailsLiveData = viewModel.getInboundDetails(editInboundId)
        detailsLiveData.observe(this) { value ->
            if (isDataLoaded || value.isEmpty()) return@observe

            tableItems.removeAllViews()
            tempItemsList.clear()

            for (detailWithItem in value) {
                val detail = InboundDetails(
                    id = 0, // نتركه 0 لأننا في الـ Repo نقوم بحذف القديم وإضافة الجديد
                    InboundId = editInboundId.toInt(),
                    ItemId = detailWithItem.itemId.toInt(),
                    amount = detailWithItem.quantity,
                    userId = Prefs.getUserId(this@AddInboundActivity) ?: ""
                )
                tempItemsList.add(detail)
                addTableRow(detail, detailWithItem.itemName)
            }
            isDataLoaded = true
        }
    }
    private fun checkAndAddItem() {
        val qty = etQuantity.text.toString()
        if (selectedItem == null) {
            autoItem.error = "يجب اختيار صنف"
            return
        }
        var result="0"
        if (qty.isNotEmpty()) {
             result = calculateResult(qty)
            etQuantity.setText(result)
            // لنقل المؤشر لآخر الرقم
            etQuantity.setSelection(etQuantity.text.length)
        }else{
            etQuantity.error = "يجب اختيار صنف"
            return
        }

        val detail = InboundDetails(
            id = 0,
            InboundId = if (isEditMode) editInboundId.toInt() else 0,
            ItemId = selectedItem!!.id,
            amount = result.toInt(),
            userId = Prefs.getUserId(this@AddInboundActivity) ?: ""
        )

        tempItemsList.add(detail)
        addTableRow(detail, selectedItem!!.itemName)

        autoItem.text.clear()
        etQuantity.text.clear()
        selectedItem = null

// إعادة تفعيل الاقتراحات
        autoItem.requestFocus()
        autoItem.post {
            (autoItem.adapter as? ArrayAdapter<*>)?.filter?.filter(null)
            autoItem.showDropDown()
        }
    }

    private fun addTableRow(itemData: InboundDetails, selectName: String) {
        val row = TableRow(this).apply {
            setPadding(8, 16, 8, 16)
            gravity = Gravity.CENTER_VERTICAL
        }

        row.addView(TextView(this).apply {
            text = selectName
            layoutParams = TableRow.LayoutParams(0, -2, 1.2f)
        })

        row.addView(TextView(this).apply {
            text = itemData.amount.toString()
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, -2, 0.5f)
        })

        row.addView(ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setOnClickListener {
                tableItems.removeView(row)
                tempItemsList.remove(itemData) // التأكد من حذف البيانات الفعلية
            }
        })
        tableItems.addView(row)
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
    }

    private fun fillTableFromAI(jsonString: String) {
        try {
            val items = JSONArray(jsonString)
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                addNewRowToTable(item.getString("itemName"), item.getInt("quantity"), 0.0)
            }
        } catch (e: Exception) {
            Log.e("AI_FILL", "Error", e)
        }
    }

    private fun addNewRowToTable(name: String, qty: Int, price: Double) {
        val row = TableRow(this)
        row.addView(EditText(this).apply { setText(name); layoutParams = TableRow.LayoutParams(0, -2, 2f) })
        row.addView(EditText(this).apply { setText(qty.toString()); layoutParams = TableRow.LayoutParams(0, -2, 1f) })
        row.addView(ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setOnClickListener { tableItems.removeView(row) }
        })
        tableItems.addView(row)
    }
}