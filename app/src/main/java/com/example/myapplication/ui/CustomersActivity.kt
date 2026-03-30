package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.OutboundRepoImpl
import com.example.myapplication.domin.model.Customer
import com.example.myapplication.domin.useCase.FetchRemoteOutboundsUseCase
import com.example.myapplication.domin.useCase.ProcessOutboundUseCase
import com.example.myapplication.ui.factory.OutboundViewModelFactory
import com.example.myapplication.ui.outbound.OutboundViewModel

class CustomersActivity : AppCompatActivity() {

    private lateinit var viewModel: OutboundViewModel // سنستخدم نفس الـ ViewModel أو واحد مخصص للعملاء
    private lateinit var rvCustomers: RecyclerView
    private lateinit var tvTotalDebt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customers)

        // إعداد الـ ViewInsets كما في كودك الأصلي
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupViewModel()
        observeData()
    }

    private fun initViews() {
        rvCustomers = findViewById(R.id.rvCustomers)
        tvTotalDebt = findViewById(R.id.tvTotalDebt)
        rvCustomers.layoutManager = LinearLayoutManager(this)
    }

    private fun setupViewModel() {
        // يمكنك استخدام نفس الـ Factory الذي أنشأناه سابقاً
        val database = AppDatabase.getDatabase(this)
        val outboundRepo = OutboundRepoImpl(
            database.outboundDao(),
            database.outboundDetailesDao(),
            database.stockDao(),
            database.itemsDao(),
            database.customerDao()
        )
        val customerRepo = CustomerRepoImpl(database.customerDao())
        val fetchRemoteUseCase =FetchRemoteOutboundsUseCase(outboundRepo)

        val factory = OutboundViewModelFactory(
            fetchRemoteUseCase,
            ProcessOutboundUseCase(outboundRepo),
            outboundRepo,
            customerRepo
        )

        viewModel = ViewModelProvider(this, factory)[OutboundViewModel::class.java]
    }

    private fun observeData() {
        viewModel.allCustomers(this@CustomersActivity).observe(this) { customers ->
            // حساب إجمالي الديون
            val total = customers.sumOf { it.customerDebt?.toDouble() ?: 0.0 }
            tvTotalDebt.text = String.format("%.2f ج.م", total)

            // إعداد الـ Adapter (سنقوم بإنشاء الـ Adapter بسيط هنا)
            setupAdapter(customers)
        }
    }

    private fun setupAdapter(customers: List<Customer>) {
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_customer_balance, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val customer = customers[position]

                // ربط البيانات بالواجهة
                holder.itemView.findViewById<TextView>(R.id.txtName).text = customer.customerName
                holder.itemView.findViewById<TextView>(R.id.txtBalance).text = "${customer.customerDebt} ج.م"

                // --- الكود الجديد للانتقال لصفحة كشف الحساب ---
                holder.itemView.setOnClickListener {
                    val intent = android.content.Intent(this@CustomersActivity,
                        com.example.myapplication.ui.customerState.CustomerStatementActivity::class.java)

                    // تمرير المعرف والاسم (تأكد أن الأسماء مطابقة لما يستقبله النشاط الآخر)
                    intent.putExtra("CUSTOMER_ID", customer.id)
                    intent.putExtra("CUSTOMER_NAME", customer.customerName)

                    startActivity(intent)
                }
            }

            override fun getItemCount() = customers.size
        }
        rvCustomers.adapter = adapter
    }}