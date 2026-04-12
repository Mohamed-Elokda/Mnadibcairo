package com.example.myapplication.ui.inbound

import android.R.attr.padding
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.data.formatToEnglish
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.domin.model.Inbound
import com.example.myapplication.domin.useCase.AddInboundUseCase
import com.example.myapplication.domin.useCase.GetInboundDetailsUseCase

class InboundActivity : AppCompatActivity() {

    private lateinit var tableInbound: TableLayout
    // استخدم ViewModelProvider للحصول على نسخة من الـ ViewModel
    // 1. تجهيز المتطلبات (يفضل استخدام Dependency Injection مستقبلاً مثل Hilt)
    private val viewModel: InboundViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)

        // تحويل الـ DAOs إلى Repository Implementation
        val repository = InboundRepositoryImpl(
            database.inboundDao(),
            database.inboundDetailesDao(),
            database.stockDao(),
            database.suppliedDao(),
            database.itemsDao()
        )
        val getInboundDetailsUseCase = GetInboundDetailsUseCase(repository)

        val addInboundUseCase = AddInboundUseCase(repository)

        InboundViewModelFactory(addInboundUseCase, getInboundDetailsUseCase,repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbound)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnAddInbound = findViewById<Button>(R.id.btnAddInbound)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        tableInbound = findViewById(R.id.tableInbound)

        btnBack.setOnClickListener { finish() }

        viewModel.getInbounds(Prefs.getUserId(this)!!).observe(this) { inboundList ->
            try {
                displayInboundData(inboundList)
            }catch (ex: Exception){
                Toast.makeText(this@InboundActivity,ex.message,Toast.LENGTH_SHORT).show(  )
            }

        }
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString()
                viewModel.getInbounds(Prefs.getUserId(this@InboundActivity)!!).observe(this@InboundActivity) { list ->
                    val filteredList = list.filter { it.invorseNum.toString().contains(query)|| it.suppliedName.toString().contains(query) }
                    displayInboundData(filteredList)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
btnAddInbound.setOnClickListener {
    startActivity(Intent(this, AddInboundActivity::class.java))
}




    }
    private fun displayInboundData(inboundList: List<Inbound>) {
        // 1. مسح الصفوف القديمة مع الحفاظ على صف العنوان
        val childCount = tableInbound.childCount
        if (childCount > 1) {
            tableInbound.removeViews(1, childCount - 1)
        }

        // 2. إضافة صف لكل فاتورة
        for (inbound in inboundList) {
            val row = TableRow(this).apply {
                setPadding(8, 16, 8, 16) // زيادة البادنج الرأسي لتسهيل الضغط
                background = ContextCompat.getDrawable(this@InboundActivity, android.R.drawable.list_selector_background)
                isClickable = true
                isFocusable = true
            }

            // رقم الفاتورة (كـ نص يظهر للمستخدم)
            val tvId = TextView(this).apply {

                text = inbound.invorseNum.toString()
                setPadding(8, 8, 8, 8)
            }

            // التاريخ
            val tvDate = TextView(this).apply {
                text = inbound.inboundDate.substringBefore("T") // لعرض التاريخ فقط بدون الوقت
                setPadding(8, 8, 8, 8)
            }

            // اسم المورد
            val tvSupplier = TextView(this).apply {
                text = inbound.suppliedName.toString() // يفضل لاحقاً عمل Join لعرض الاسم بدلاً من الرقم
                setPadding(8, 8, 8, 8)
            }

            row.addView(tvId)
            row.addView(tvDate)
            row.addView(tvSupplier)
            row.setOnLongClickListener { view ->
                showUpdateDeleteMenu(view, inbound)
                true // تعني أننا استهلكنا الحدث ولن يتم تنفيذ النقرة العادية
            }
            // --- التعديل هنا: الانتقال لصفحة التفاصيل ---
            row.setOnClickListener {
                val intent = Intent(this, InboundDetailActivity::class.java).apply {
                    // تمرير المعرف (ID) وهو الأهم لجلب البيانات من Room
                    putExtra("INBOUND_ID", inbound.id.toLong())

                    // تمرير البيانات المتاحة لتقليل الضغط على قاعدة البيانات في الشاشة التالية
                    putExtra("INVOICE_NUM", inbound.invorseNum.toString())
                    putExtra("SUPPLIER", inbound.suppliedName)
                    putExtra("DATE", inbound.inboundDate)

                    // إذا كانت بيانات الصور والمبالغ متوفرة في كائن Inbound مررها أيضاً
                    // putExtra("IMAGE_URL", inbound.imageUrl)
                    // putExtra("TOTAL_AMOUNT", inbound.totalAmount)
                }
                startActivity(intent)
            }

            tableInbound.addView(row)
        }
    }
    private fun openExcelFile(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "${packageName}.provider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            startActivity(Intent.createChooser(intent, "فتح ملف التقرير بواسطة:"))
        } catch (e: Exception) {
            Toast.makeText(this, "لا يوجد تطبيق لفتح Excel", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showUpdateDeleteMenu(view: android.view.View, inbound: Inbound) {
        val popup = androidx.appcompat.widget.PopupMenu(this, view)

        // 1. تجهيز تاريخ اليوم بالإنجليزية
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
        val currentDateStr = sdf.format(java.util.Date())

        // 2. تنظيف تاريخ الفاتورة وتحويل أرقامه للإنجليزية
        val rawDate = inbound.inboundDate.substringBefore("T").trim()
        val cleanedInvoiceDate = formatToEnglishDigits(rawDate)

        // 3. التحقق من الشرط (نفس اليوم)
        if (currentDateStr == cleanedInvoiceDate) {
            popup.menu.add("تعديل")
            popup.menu.add("حذف")
        } else {
            // اختياري: إظهار خيار "عرض" فقط أو رسالة توضيحية
            val item = popup.menu.add("لا يمكن التعديل/الحذف (تاريخ قديم)")
            item.isEnabled = false
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "تعديل" -> {
                    openEditInbound(inbound)
                }
                "حذف" -> {
                    showDeleteConfirmationDialog(inbound)
                }
            }
            true
        }
        popup.show()
    }
    private fun formatToEnglishDigits(input: String): String {
        var result = input
        val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val englishChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

        for (i in 0..9) {
            result = result.replace(arabicChars[i], englishChars[i])
        }
        return result
    }
    private fun openEditInbound(inbound: Inbound) {
        val intent = Intent(this, AddInboundActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("INBOUND_ID", inbound.id.toLong())
            putExtra("INVOICE_NUM", inbound.invorseNum.toString())
            putExtra("SUPPLIER", inbound.fromSppliedId)
            // يمكنك تمرير باقي البيانات حسب الحاجة
        }
        startActivity(intent)
    }
    private fun showDeleteConfirmationDialog(inbound: Inbound) {
        android.app.AlertDialog.Builder(this)
            .setTitle("تأكيد الحذف")
            .setMessage("هل أنت متأكد من حذف الفاتورة رقم ${inbound.invorseNum}؟")
            .setPositiveButton("نعم") { _, _ ->
                viewModel.deleteInboundWithDetails(inbound)            }
            .setNegativeButton("إلغاء", null)
            .show()
    }


}





