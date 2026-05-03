package com.example.myapplication.ui.returned

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TableRow
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.core.formatToEnglishDigits
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.core.showUpdateDeleteMenu
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.ReturnedRepoImpl
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.model.ReturnedWithNameModel
import com.example.myapplication.domin.useCase.AddReturnedUseCase
import com.example.myapplication.domin.useCase.GetAllCustomersUseCase
import com.example.myapplication.domin.useCase.GetAllReturnedUseCase
import com.example.myapplication.domin.useCase.GetCustomerItemsUseCase
import com.example.myapplication.domin.useCase.GetItemHistoryUseCase
import com.example.myapplication.domin.useCase.GetLastPriceUseCase
import com.example.myapplication.domin.useCase.GetReturnedDetailsUseCase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ReturnedActivity : AppCompatActivity() {

    // 1. تعريف الـ ViewModel باستخدام الـ Factory
    private val viewModel: ReturnedViewModel by viewModels ()

    private lateinit var adapter: ReturnedAdapter
    private var fullList = listOf<ReturnedWithNameModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_returned)

        setupRecyclerView()
        observeViewModel()
        setupSearchAndAdd()

        scheduleSync(context = this)

    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvReturnedInvoices)
        adapter = ReturnedAdapter(
            onItemClick = { item ->
                val intent = Intent(this, ReturnedDetailsActivity::class.java).apply {
                    // نرسل الـ ID والبيانات الأساسية لتعرضها شاشة التفاصيل فوراً
                    putExtra("RETURNED_ID", item.returnedModel.id)
                    putExtra("RETURNED_Num", item.returnedModel.invoiceNum)
                    putExtra("CUSTOMER_NAME", item.customerName)
                    putExtra("DATE", item.returnedModel.returnedDate)
                }
                startActivity(intent)
            },
            onLongClick = { item ->
                // إظهار منيو (تعديل / حذف)
                showUpdateDeleteMenu(this, rv, item, {
                    openEditReturned(item) // تنفيذ التعديل
                }, {
                    showDeleteConfirmationDialog(item) // تنفيذ الحذف
                })
            }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun showDeleteConfirmationDialog(item:  ReturnedWithNameModel) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("حذف صنف")
            .setMessage("هل أنت متأكد من حذف ${item.customerName} من قائمة المرتجع؟")
            .setPositiveButton("حذف") { dialog, _ ->
                // 1. إزالة السطر من الجدول (UI)
                viewModel.deleteReturned(item)

                Toast.makeText(this, "تم حذف الصنف", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("إلغاء") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun openEditReturned(item: ReturnedWithNameModel) {
        val intent = Intent(this, AddReturnedActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("RETURNED_ID", item.returnedModel.id)
            putExtra("CUSTOMER_NAME", item.customerName)
            putExtra("CUSTOMER_ID", item.returnedModel.customerId)
            putExtra("DATE", item.returnedModel.returnedDate)
        }
        startActivity(intent)
    }

    public fun showUpdateDeleteMenu(context: Context, view: View, invoice:   ReturnedWithNameModel,editClick:(invoice:  ReturnedWithNameModel)-> Unit,deleteClick:(invoice:  ReturnedWithNameModel)-> Unit) {
        val popup = PopupMenu(context, view)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val currentDateStr = sdf.format(Date())

        val rawDate = invoice.returnedModel.returnedDate.take(10).trim()
        val cleanedInvoiceDate = formatToEnglishDigits(rawDate)

//    if (currentDateStr == cleanedInvoiceDate) {
        popup.menu.add("تعديل")
        popup.menu.add("حذف")
//    } else {
//        val item = popup.menu.add("لا يمكن التعديل/الحذف (تاريخ قديم)")
//        item.isEnabled = false
//    }



        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "تعديل" -> editClick(invoice)
                "حذف" -> deleteClick(invoice)
            }
            true
        }
        popup.show()
    }

    private fun observeViewModel() {
        // 2. مراقبة قائمة المرتجعات وتحديث الـ Adapter
        lifecycleScope.launch {
            viewModel.returnedList.collect { list ->
                fullList = list
                adapter.submitList(list)
            }
        }
    }

    private fun setupSearchAndAdd() {
        val etSearch = findViewById<EditText>(R.id.etSearchReturned)
        val btnAdd = findViewById<FloatingActionButton>(R.id.btnAddReturn)

        // منطق البحث
        etSearch.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            val filtered = fullList.filter {
                it.customerName.lowercase().contains(query) ||
                        it.itemName.lowercase().contains(query)
            }
            adapter.submitList(filtered)
        }

        // الانتقال لشاشة إضافة مرتجع جديد
        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddReturnedActivity::class.java))
        }
    }
}