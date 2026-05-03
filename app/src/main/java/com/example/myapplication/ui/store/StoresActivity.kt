package com.example.myapplication.ui.store

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.data.repository.StockRepoImpl
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.useCase.inboundUseCases.AddInboundUseCase
import com.example.myapplication.domin.useCase.inboundUseCases.GetInboundDetailsUseCase
import com.example.myapplication.domin.useCase.GetStockFromServer
import com.example.myapplication.domin.useCase.GetStockUseCase
import com.example.myapplication.ui.inbound.InboundViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory

@AndroidEntryPoint
class StoresActivity : AppCompatActivity() {


    // لانشر اختيار ملف الإكسيل
    private val excelPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { readExcelFile(it) }
        }
    private lateinit var etSearch: EditText
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalStock: TextView
    private lateinit var adapter: InventoryAdapter

    // ViewModel الخاص بالاستيراد (Inbound)
    private val inboundViewModel: InboundViewModel by viewModels()
    private lateinit var items: List<Stock>

    // ViewModel الخاص بعرض المخزن (Store)
    // داخل StoresActivity.kt
    private val storeViewModel: StoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stores)

        initViews()
        setupObservers()

        storeViewModel.refreshStockItems()
        etSearch.addTextChangedListener { text ->
            val filteredList = items.filter {
                it.itemName.contains(text.toString(), ignoreCase = true)
            }
            adapter.updateList(filteredList)

            // تحديث إحصائيات البحث (اختياري)
        }
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        tvTotalItems = findViewById(R.id.tvTotalItems)
        tvTotalStock = findViewById(R.id.tvTotalStock)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }


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
                            items = state.items
                        }

                        is StoreState.Error -> {
                            Toast.makeText(this@StoresActivity, state.message, Toast.LENGTH_SHORT)
                                .show()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupRecyclerView(stockList: List<Stock>) {
        val recyclerView = findViewById<RecyclerView>(R.id.rvInventory)
        adapter = InventoryAdapter(stockList, {
            val intent = Intent(this, StockMovementActivity::class.java).apply {
                // نرسل الـ ID والاسم للشاشة التالية
                putExtra("ITEM_ID", it.ItemId)
                putExtra("ITEM_NAME", it.itemName)
            }
            startActivity(intent)
        })

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // تفعيل الأنيميشن برمجياً
        val resId = R.anim.layout_animation_fall_down
        val animation = AnimationUtils.loadLayoutAnimation(this, resId)
        recyclerView.layoutAnimation = animation

        // لإعادة تشغيل الأنيميشن عند تحديث البيانات
        recyclerView.scheduleLayoutAnimation()
    }

    private fun displayStockData(stockList: List<Stock>) {
        // تنظيف الجدول من البيانات القديمة مع الإبقاء على الهيدر (الصف الأول)


        setupRecyclerView(stockList)
        tvTotalItems.text = stockList.size.toString()
        var sum: Int = 0
        stockList.forEach {
            sum += it.InitAmount
        }
        tvTotalStock.text = sum.toString()
        // إضافة الأعمدة


    }

    private fun createTextView(txt: String, isBold: Boolean = false): TextView {
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
                        Toast.makeText(
                            this@StoresActivity,
                            "تم استيراد ${itemsToInsert.size} صنف بنجاح",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                workbook.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@StoresActivity, "خطأ في الملف: ${e.message}", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}