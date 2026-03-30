package com.example.myapplication.ui.outbound

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.local.entity.OutboundEntity
import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.OutboundRepoImpl
import com.example.myapplication.domin.model.Outbound // تأكد من استيراد الـ Domain Model
import com.example.myapplication.domin.useCase.FetchRemoteOutboundsUseCase
import com.example.myapplication.domin.useCase.ProcessOutboundUseCase
import com.example.myapplication.ui.factory.OutboundViewModelFactory // ستحتاج لـ Factory لتمري الـ Repo
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class OutboundActivity : AppCompatActivity() {

    private lateinit var viewModel: OutboundViewModel
    private var allInvoices = listOf<Outbound>() // القائمة الأصلية للبحث
    private lateinit var tableOutbound: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outbound)
        scheduleSync(context = this)

        // 1. تعريف العناصر من الواجهة (XML)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnAddOutbound = findViewById<Button>(R.id.btnAddOutbound)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        tableOutbound = findViewById(R.id.tableOutbound)

        // 2. تهيئة الـ ViewModel
        // ملاحظة: ستحتاج لـ Factory لأن الـ ViewModel يأخذ معاملات (Repository/UseCase)
        initViewModel()

        // 3. مراقبة البيانات (Observe)
        viewModel.allInvoices.observe(this) { invoices ->
            this.allInvoices = invoices // تحديث القائمة المحلية لاستخدامها في البحث
            displayInvoices(invoices)   // عرض البيانات في الجدول
        }

        // 4. جلب البيانات من قاعدة البيانات
        viewModel.loadInvoices(this@OutboundActivity)
viewModel.refreshCustomers(Prefs.getUserId(this@OutboundActivity)?:"");
        // 5. إعدادات الأزرار والبحث
        btnBack.setOnClickListener { finish() }
        viewModel.syncWithServer(this)
        btnAddOutbound.setOnClickListener {
            startActivity(Intent(this, AddOutboundActivity::class.java))
        }
        viewModel.checkAndSyncItemsIfEmpty()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterInvoices(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

// داخل OutboundActivity.kt
private fun showUpdateDeleteMenu(view: android.view.View, invoice: Outbound) {
    val popup = androidx.appcompat.widget.PopupMenu(this, view)

    // 1. جلب تاريخ اليوم بالإنجليزية (yyyy-MM-dd)
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
    val currentDateStr = sdf.format(java.util.Date())

    // 2. تنظيف تاريخ الفاتورة (أخذ أول 10 حروف وتحويل الأرقام للإنجليزية)
    val rawDate = invoice.outboundDate.take(10).trim()
    val cleanedInvoiceDate = formatToEnglishDigits(rawDate)

    // 3. الشرط: يظهر التعديل والحذف فقط إذا كان التاريخ هو اليوم
    if (currentDateStr == cleanedInvoiceDate) {
        popup.menu.add("تعديل")
        popup.menu.add("حذف")
    } else {
        val item = popup.menu.add("لا يمكن التعديل/الحذف (تاريخ قديم)")
        item.isEnabled = false
    }

    popup.setOnMenuItemClickListener { item ->
        when (item.title) {
            "تعديل" -> openEditOutbound(invoice)
            "حذف" -> showDeleteConfirmationDialog(invoice)
        }
        true
    }
    popup.show()
}
    private fun openEditOutbound(invoice: Outbound) {
        val intent = Intent(this, AddOutboundActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("OUTBOUND_ID", invoice.id.toString())
            // مرر أي بيانات أخرى تحتاجها في شاشة الإضافة/التعديل
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmationDialog(invoice: Outbound) {
        android.app.AlertDialog.Builder(this)
            .setTitle("تأكيد الحذف")
            .setMessage("هل أنت متأكد من حذف فاتورة رقم ${invoice.invorseNumber}؟ سيتم إعادة الأصناف للمخزن تلقائياً.")
            .setPositiveButton("نعم") { _, _ ->
                // استدعاء الحذف الفعلي
                viewModel.deleteInvoice(invoice,this)
                Toast.makeText(this, "تم الحذف وإعادة الكميات للمخزن", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }    // دالة مساعدة لتوحيد الأرقام (لضمان نجاح المقارنة)
    private fun formatToEnglishDigits(input: String): String {
        var result = input
        val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val englishChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        for (i in 0..9) {
            result = result.replace(arabicChars[i], englishChars[i])
        }
        return result
    }
    private fun initViewModel() {
        val database = AppDatabase.getDatabase(this)

        // إرسال الـ 5 معاملات المطلوبة للـ OutboundRepoImpl
        val outboundRepo = OutboundRepoImpl(
            database.outboundDao(),
            database.outboundDetailesDao(),
            database.stockDao(),
            database.itemsDao(), // لا تنسى هذا
            database.customerDao()
        )

        val customerRepo = CustomerRepoImpl(database.customerDao())

        // إنشاء الـ UseCase (تأكد من أنه يقبل outboundRepo و customerRepo أو حسب تعديلك الأخير)
        val processUseCase = ProcessOutboundUseCase(outboundRepo)
        val fetchRemoteUseCase =FetchRemoteOutboundsUseCase(outboundRepo)

        // تمرير المعاملات الثلاثة للفاكتوري
        val factory = OutboundViewModelFactory(fetchRemoteUseCase,processUseCase, outboundRepo, customerRepo)
        viewModel = ViewModelProvider(this, factory)[OutboundViewModel::class.java]
    }

    private fun filterInvoices(query: String) {
        val filteredList = allInvoices.filter {
            it.invorseNumber.toString().contains(query) ||
                    it.customerId.toString().contains(query)
        }
        displayInvoices(filteredList)
    }


    private fun displayInvoices(invoices: List<Outbound>) {
        // مسح الصفوف القديمة مع الحفاظ على الهيدر (Index 0)
        val childCount = tableOutbound.childCount
        if (childCount > 1) {
            tableOutbound.removeViews(1, childCount - 1)
        }

        if (invoices.isEmpty()) {
            Toast.makeText(this, "لا توجد فواتير صادرة حالياً", Toast.LENGTH_SHORT).show()
            return
        }

        invoices.forEach { invoice ->
            val row = TableRow(this).apply {
                setPadding(0, 20, 0, 20)
                background = getDrawable(android.R.drawable.list_selector_background)
                isClickable = true
                isFocusable = true
                gravity = Gravity.CENTER_VERTICAL
            }

            // 1. رقم الفاتورة
            row.addView(createTextView(invoice.invorseNumber.toString()))

            // 2. اسم العميل (أو المعرف حالياً)
            row.addView(createTextView(invoice.customerName))

            // 3. التاريخ
            val formattedDate = invoice.outboundDate.substringBefore(" ")
            row.addView(createTextView(formattedDate))

            // 4. المبلغ المستلم
            row.addView(createTextView("${invoice.moneyResive} ج.م"))
            row.setOnLongClickListener { view ->
                showUpdateDeleteMenu(view, invoice)
                true
            }
            row.setOnClickListener {
                val intent = Intent(this, OutboundDetailActivity::class.java).apply {
                    // نستخدم String دائماً للمفاتيح لتجنب الارتباك
                    putExtra("OUTBOUND_ID", invoice.id.toString()) // حولناه لـ String
                    putExtra("INVOICE_NUM", invoice.invorseNumber.toString())
                    putExtra("CUSTOMER_NAME", invoice.customerName) // أو اسم العميل إذا توفر
                    putExtra("IMAGE_URL", invoice.image)
                    putExtra("DATE", invoice.outboundDate)
                    putExtra("PAID_AMOUNT", invoice.moneyResive.toString())
                }
                startActivity(intent)
            }

            tableOutbound.addView(row)
        }
    }


    // داخل OutboundViewModel.kt

    // دالة مساعدة لإنشاء الـ TextView بتنسيق موحد
    private fun createTextView(txt: String): TextView {
        return TextView(this).apply {
            text = txt
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.black)) // تأكد من وجود اللون في colors.xml
            textSize = 14f
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

}