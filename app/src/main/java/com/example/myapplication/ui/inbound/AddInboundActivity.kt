package com.example.myapplication.ui.inbound

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.example.myapplication.domin.useCase.inboundUseCases.AddInboundUseCase
import com.example.myapplication.domin.useCase.inboundUseCases.GetInboundDetailsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
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
    private var editInboundId: String = ""
    private var originalDate: String? = null
    private var SUPPLIER: String? = null
    private var isDataLoaded = false
    private lateinit var invoiceScrollView: ScrollView // تعريف المتغير
    private val viewModel: InboundViewModel by viewModels()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_inbound)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainadd)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 1. ربط العناصر بالـ XML أولاً
        initViews()
        val mainLayout = findViewById<ConstraintLayout>(R.id.mainadd)
        val keyboardSpace = findViewById<View>(R.id.keyboardSpace)

        mainLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            mainLayout.getWindowVisibleDisplayFrame(r)
            val screenHeight = mainLayout.rootView.height
            val keypadHeight = screenHeight - r.bottom

            // إذا كان الكيبورد مفتوحاً (أكثر من 200 بكسل)
            if (keypadHeight > 200) {
                val params = keyboardSpace.layoutParams
                params.height = keypadHeight - 200 // نعطي مساحة مساوية للكيبورد
                keyboardSpace.layoutParams = params
            } else {
                // إرجاع المساحة للصفر عند إغلاق الكيبورد
                val params = keyboardSpace.layoutParams
                params.height = 0
                keyboardSpace.layoutParams = params
            }
        }
        // 2. تصفية البيانات القديمة
        tempItemsList.clear()

        // 3. إعداد المراقبين (Observers)
        setupObservers()

        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        if (isEditMode) {
            editInboundId = intent.getStringExtra("INBOUND_ID")?: UUID.randomUUID().toString()
            SUPPLIER = intent.getStringExtra("suppliedName")
            selectedFromSuppliedId = intent.getIntExtra("SUPPLIER",-1)
            originalDate = intent.getStringExtra("DATE")
            autoFromSupplied.setText(SUPPLIER)
            setupEditMode()
        }
        Log.d("TAG", "onCreate: "+isEditMode)

        findViewById<Button>(R.id.btnAddItem).setOnClickListener { checkAndAddItem() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveFullInvoice() }

        intent.getStringExtra("AI_ITEMS_JSON")?.let { fillTableFromAI(it) }
    }

    private fun initViews() {
        etInvoiceNumber = findViewById(R.id.etInvoiceNumber)
        autoItem = findViewById(R.id.autoItem)
        etQuantity = findViewById(R.id.etQuantity)
        tableItems = findViewById(R.id.tableItems)
        invoiceScrollView = findViewById(R.id.invoiceScrollView) // الربط


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
            id = if (isEditMode) editInboundId else UUID.randomUUID().toString(),
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
                    id = UUID.randomUUID()
                        .toString(), // نتركه 0 لأننا في الـ Repo نقوم بحذف القديم وإضافة الجديد
                    InboundId = editInboundId,
                    ItemId = detailWithItem.itemId.toInt(),
                    amount = detailWithItem.quantity,
                    userId = Prefs.getUserId(this@AddInboundActivity) ?: "",
                    updated_at = System.currentTimeMillis(),
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
            id = UUID.randomUUID().toString(),
            InboundId = if (isEditMode) editInboundId else UUID.randomUUID().toString(),
            ItemId = selectedItem!!.id,
            amount = result.toInt(),
            userId = Prefs.getUserId(this@AddInboundActivity) ?: "",
            updated_at = System.currentTimeMillis(),

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
            setPadding(4, 8, 4, 8)
            background = getDrawable(android.R.drawable.editbox_dropdown_light_frame) // خلفية بسيطة للصف
        }

        // 1. اسم الصنف
        row.addView(TextView(this).apply {
            text = selectName
            textSize = 14f
            setPadding(10, 10, 10, 10)
            layoutParams = TableRow.LayoutParams(0, -2, 1.2f)
        })

        // 2. كمية قابلة للتعديل (EditText)
        val etQty = EditText(this).apply {
            setText(itemData.amount.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            background = null // إزالة الخط السفلي لتصميم أنظف
            setPadding(10, 10, 10, 10)
            layoutParams = TableRow.LayoutParams(0, -2, 0.5f)

            // تحديث القيمة في القائمة عند التغيير
            addTextChangedListener { text ->
                val newQty = text.toString().toIntOrNull() ?: 0
                itemData.amount = newQty // تحديث الكائن مباشرة في tempItemsList
            }
            setOnClickListener {

                // نستخدم postDelayed عشان ندي وقت للكيبورد يظهر والـ Layout يتعدل
                invoiceScrollView.postDelayed({
                    // حساب المسافة: نريد الصف أن يكون في أعلى الـ ScrollView
                    // row.top تعطيك مكانه بالنسبة للـ Table
                    // ونطرح منها مساحة بسيطة (مثلاً 100 بكسل) عشان ما يلزقش في السقف
                    val scrollTarget = row.top+500

                    invoiceScrollView.smoothScrollTo(0, scrollTarget)

                    // لزيادة التأكيد، اطلب من الحقل "طلب التركيز" برمجياً
                }, 400) // 500ms تضمن إن الكيبورد أخد مساحته تماماً
            }

            setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    // نستخدم postDelayed عشان ندي وقت للكيبورد يظهر والـ Layout يتعدل
                    invoiceScrollView.postDelayed({
                        // حساب المسافة: نريد الصف أن يكون في أعلى الـ ScrollView
                        // row.top تعطيك مكانه بالنسبة للـ Table
                        // ونطرح منها مساحة بسيطة (مثلاً 100 بكسل) عشان ما يلزقش في السقف
                        val scrollTarget = row.top+500

                        invoiceScrollView.smoothScrollTo(0, scrollTarget)

                        // لزيادة التأكيد، اطلب من الحقل "طلب التركيز" برمجياً
                        view.requestFocus()
                    }, 500) // 500ms تضمن إن الكيبورد أخد مساحته تماماً
                }
            }
        }
        row.addView(etQty)

        // 3. زر الحذف
        row.addView(ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            background = null
            setColorFilter(Color.RED)
            setOnClickListener {
                tableItems.removeView(row)
                tempItemsList.remove(itemData)
            }
        })

        tableItems.addView(row)
    }
    private fun getCurrentDate(): String {
        return  SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date())
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