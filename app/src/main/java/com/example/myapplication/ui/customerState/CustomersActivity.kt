package com.example.myapplication.ui.customerState

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.domin.model.CustomerModel
import com.example.myapplication.ui.outbound.OutboundViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomersActivity : AppCompatActivity() {

    private  val viewModel: OutboundViewModel by viewModels()
    private val StatementViewModel: StatementViewModel by viewModels()

    private lateinit var rvCustomers: RecyclerView
    private lateinit var tvTotalDebt: TextView
    private lateinit var adapter: CustomerAdapter
    private var fullList: List<CustomerModel> = listOf()
    private lateinit var etSearch: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customers)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        StatementViewModel.refreshCustomersDept()
        adapter = CustomerAdapter(fullList) { customer ->
            val intent = Intent(this@CustomersActivity,
                CustomerStatementActivity::class.java)

            intent.putExtra("CUSTOMER_ID", customer.id)
            intent.putExtra("CUSTOMER_NAME", customer.customerName)

            startActivity(intent)



        }

        rvCustomers.layoutManager = LinearLayoutManager(this)
        rvCustomers.adapter = adapter
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filteredList = fullList.filter {
                    it.customerName.lowercase().contains(query)
                }
                adapter.updateList(filteredList)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        observeData()
    }

    private fun initViews() {
        rvCustomers = findViewById(R.id.rvCustomers)
        tvTotalDebt = findViewById(R.id.tvTotalDebt)
        etSearch = findViewById(R.id.etSearch)
        rvCustomers.layoutManager = LinearLayoutManager(this)
    }




    private fun observeData() {
        viewModel.allCustomers(this@CustomersActivity).observe(this) { list ->
            fullList = list
            adapter.updateList(list)
            updateTotalDebt(list)
        }
    }
    private fun filter(text: String) {
        val filteredList = fullList.filter { customer ->
            customer.customerName.contains(text, ignoreCase = true)
        }
        adapter.updateList(filteredList)
    }

    private fun updateTotalDebt(list: List<CustomerModel>) {
        val total = list.sumOf { it.customerDebt }
        findViewById<TextView>(R.id.tvTotalDebt).text = "${String.format("%.2f", total)} ج.م"
    }
    private fun setupAdapter(customerModels: List<CustomerModel>) {
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_customer_balance, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val customer = customerModels[position]

                holder.itemView.findViewById<TextView>(R.id.txtName).text = customer.customerName
                holder.itemView.findViewById<TextView>(R.id.txtBalance).text = "${customer.customerDebt} ج.م"

                holder.itemView.setOnClickListener {
                    val intent = Intent(this@CustomersActivity,
                        CustomerStatementActivity::class.java)

                    intent.putExtra("CUSTOMER_ID", customer.id)
                    intent.putExtra("CUSTOMER_NAME", customer.customerName)

                    startActivity(intent)
                }
            }

            override fun getItemCount() = customerModels.size
        }
        rvCustomers.adapter = adapter
    }}