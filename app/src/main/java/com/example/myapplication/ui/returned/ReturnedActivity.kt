package com.example.myapplication.ui.returned

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.ReturnedRepoImpl
import com.example.myapplication.domin.model.ReturnedWithNameModel
import com.example.myapplication.domin.useCase.AddReturnedUseCase
import com.example.myapplication.domin.useCase.GetAllCustomersUseCase
import com.example.myapplication.domin.useCase.GetAllReturnedUseCase
import com.example.myapplication.domin.useCase.GetCustomerItemsUseCase
import com.example.myapplication.domin.useCase.GetItemHistoryUseCase
import com.example.myapplication.domin.useCase.GetLastPriceUseCase
import com.example.myapplication.domin.useCase.GetReturnedDetailsUseCase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ReturnedActivity : AppCompatActivity() {

    // 1. تعريف الـ ViewModel باستخدام الـ Factory
    private val viewModel: ReturnedViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = ReturnedRepoImpl(
            database,
            database.returnedDao(),
            database.returnedDetailsDao(),
            database.outboundDetailesDao(),
            database.customerDao(),
            database.stockDao()
        )
        val addUseCase = AddReturnedUseCase(repository)
        val customerRepo = CustomerRepoImpl(database.customerDao())
        val getAllReturnedUseCase = GetAllReturnedUseCase(repository)
        val getAllCustomersUseCase = GetAllCustomersUseCase(customerRepo)
        val getCustomerItemsUseCase = GetCustomerItemsUseCase(repository)
        val getLastPriceUseCase = GetLastPriceUseCase(repository)
        val getReturnedDetailsUseCase = GetReturnedDetailsUseCase(repository)
        val getItemHistoryUseCas = GetItemHistoryUseCase(repository)


        // تأكد أن الـ Factory يستقبل الـ UseCases الجديدة التي صممناها
        ReturnedViewModelFactory(
            getCustomerItemsUseCase,
            getLastPriceUseCase,
            getAllReturnedUseCase,
            getReturnedDetailsUseCase,
            addUseCase,
            getAllCustomersUseCase,
            getItemHistoryUseCas
        )
    }

    private lateinit var adapter: ReturnedAdapter
    private var fullList = listOf<ReturnedWithNameModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_returned)

        setupRecyclerView()
        observeViewModel()
        setupSearchAndAdd()

        viewModel.syncData()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvReturnedInvoices)
        adapter = ReturnedAdapter { item ->
            val intent = Intent(this, ReturnedDetailsActivity::class.java).apply {
                // نرسل الـ ID والبيانات الأساسية لتعرضها شاشة التفاصيل فوراً
                putExtra("RETURNED_ID", item.returnedModel.id)
                putExtra("CUSTOMER_NAME", item.customerName)
                putExtra("DATE", item.returnedModel.returnedDate)
            }
            startActivity(intent)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
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