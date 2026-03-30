package com.example.myapplication.ui.store

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.data.repository.StockRepoImpl
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.useCase.AddInboundUseCase
import com.example.myapplication.domin.useCase.GetInboundDetailsUseCase
import com.example.myapplication.domin.useCase.GetStockUseCase
import com.example.myapplication.ui.inbound.InboundViewModel
import com.example.myapplication.ui.inbound.InboundViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory

class StoresActivity : AppCompatActivity() {

    private lateinit var tableInventory: TableLayout

    // لانشر اختيار ملف الإكسيل
    private val excelPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { readExcelFile(it) }
    }

    // ViewModel الخاص بالاستيراد (Inbound)
    private val inboundViewModel: InboundViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repo = InboundRepositoryImpl(
            database.inboundDao(),
            database.inboundDetailesDao(),
            database.stockDao(),
            database.suppliedDao(),
            database.itemsDao()
        )
        val getInboundDetailsUseCase = GetInboundDetailsUseCase(repo)
        InboundViewModelFactory(AddInboundUseCase(repo),getInboundDetailsUseCase, repo)
    }

    // ViewModel الخاص بعرض المخزن (Store)
    private val storeViewModel: StoreViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repo = StockRepoImpl(database.inboundDao(),database.outboundDao(),database.returnedDao(),database.stockDao())
        val getStockUseCase = GetStockUseCase(repo)
        StoreViewModelFactory(getStockUseCase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stores)

        initViews()
        setupObservers()
    }

    private fun initViews() {
        tableInventory = findViewById(R.id.tableInventory)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }


        findViewById<Button>(R.id.btnAddProductFromExel).setOnClickListener {
            excelPickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }
    }

    private fun setupObservers() {
        // مراقبة حالة المخزن وعرضها في الجدول
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                storeViewModel.storeState.collect { state ->
                    when (state) {
                        is StoreState.Loading -> {
                            // يمكنك إضافة ProgressBar هنا لاحقاً
                        }
                        is StoreState.Success -> {
                            displayStockData(state.items)
                        }
                        is StoreState.Error -> {
                            Toast.makeText(this@StoresActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun displayStockData(stockList: List<Stock>) {
        // تنظيف الجدول من البيانات القديمة مع الإبقاء على الهيدر (الصف الأول)
        if (tableInventory.childCount > 1) {
            tableInventory.removeViews(1, tableInventory.childCount - 1)
        }

        for (stock in stockList) {
            val row = TableRow(this).apply {
                setPadding(0, 16, 0, 16)
                // جعل الصف قابل للنقر
                isClickable = true
                isFocusable = true
                // إضافة خلفية تعطي تأثير عند الضغط (Ripple Effect)
                val outValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
            }

            // إضافة الأعمدة
            row.addView(createTextView(stock.itemName))
            row.addView(createTextView(stock.CurrentAmount.toString(), isBold = true))
            row.addView(createTextView(stock.fristDate))

            // --- التعديل هنا: عند الضغط على الصف ---
            row.setOnClickListener {
                val intent = Intent(this, StockMovementActivity::class.java).apply {
                    // نرسل الـ ID والاسم للشاشة التالية
                    putExtra("ITEM_ID", stock.ItemId)
                    putExtra("ITEM_NAME", stock.itemName)
                }
                startActivity(intent)
            }

            tableInventory.addView(row)
        }
    }    private fun createTextView(txt: String, isBold: Boolean = false): TextView {
        return TextView(this).apply {
            text = txt
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(8, 8, 8, 8)
            if (isBold) {
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
            }
        }
    }

    private fun readExcelFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                val itemsToInsert = mutableListOf<Items>()

                // يبدأ من الصف 1 (يتخطى الهيدر)
                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    val itemNum = row.getCell(0)?.numericCellValue?.toInt() ?: 0
                    val itemName = row.getCell(1)?.stringCellValue ?: ""

                    if (itemName.isNotEmpty()) {
                        itemsToInsert.add(Items(id = 0, itemName = itemName, itemNum = itemNum))
                    }
                }

                if (itemsToInsert.isNotEmpty()) {
                    inboundViewModel.importBulkItems(itemsToInsert)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@StoresActivity, "تم استيراد ${itemsToInsert.size} صنف بنجاح", Toast.LENGTH_LONG).show()
                    }
                }
                workbook.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StoresActivity, "خطأ في الملف: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}