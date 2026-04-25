package com.example.myapplication.ui.outbound

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.core.formatToEnglishDigits
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.core.showUpdateDeleteMenu
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.domin.model.Inbound
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.ui.inbound.InboundAdapter
import com.example.myapplication.ui.inbound.InboundDetailActivity
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class OutboundActivity : AppCompatActivity() {

    private val viewModel: OutboundViewModel by viewModels()
    private var allInvoices = listOf<Outbound>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outbound)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainado)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        scheduleSync(context = this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnAddOutbound = findViewById<Button>(R.id.btnAddOutbound)
        val etSearch = findViewById<EditText>(R.id.etSearch)




        viewModel.allInvoices.observe(this) { invoices ->
            this.allInvoices = invoices
            displayInvoices(invoices)
        }

        viewModel.loadInvoices(this@OutboundActivity)
        viewModel.refreshCustomers(Prefs.getUserId(this@OutboundActivity) ?: "")
        btnBack.setOnClickListener { finish() }
        viewModel.syncWithServer(this)
        btnAddOutbound.setOnClickListener {
            startActivity(Intent(this, AddOutboundActivity::class.java))
        }
        viewModel.checkAndSyncItemsIfEmpty()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                viewModel.allInvoices
                    .observe(this@OutboundActivity) { list ->
                        val filteredList = list.filter {
                            it.customerName.toString().contains(query) || it.invorseNumber.toString()
                                .contains(query)
                        }
                        displayInvoices(filteredList)
                    }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun openEditOutbound(invoice: Outbound) {
        val intent = Intent(this, AddOutboundActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("OUTBOUND_ID", invoice.id.toString())
            putExtra("INVOICE_NUM", invoice.invorseNumber.toString())
            putExtra("CUSTOMER_NAME", invoice.customerName)
            putExtra("customerId", invoice.customerId)

            putExtra("previousDebt", invoice.previousDebt)
            putExtra("totalRemainder", invoice.totalRemainder)
            putExtra("DATE", invoice.outboundDate.take(10).trim())
            putExtra("PAID_AMOUNT", invoice.moneyResive)
            Toast.makeText(this@OutboundActivity, invoice.moneyResive.toString(), Toast.LENGTH_SHORT).show()
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmationDialog(invoice: Outbound) {
        AlertDialog.Builder(this)
            .setTitle("تأكيد الحذف")
            .setMessage("هل أنت متأكد من حذف فاتورة رقم ${invoice.invorseNumber}؟ سيتم إعادة الأصناف للمخزن تلقائياً.")
            .setPositiveButton("نعم") { _, _ ->
                viewModel.deleteInvoice(invoice, this)
                Toast.makeText(this, "تم الحذف وإعادة الكميات للمخزن", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }



    private fun filterInvoices(query: String) {
        val filteredList = allInvoices.filter {
            it.invorseNumber.toString().contains(query) ||
                    it.customerId.toString().contains(query)
        }
        displayInvoices(filteredList)
    }

    private fun setupRecyclerView(ouboundList: List<Outbound>) {
        val recyclerView = findViewById<RecyclerView>(R.id.rvInbound)

        val adapter = OutboundAdapter(
            ouboundList,
            onItemClick = { invoice ->


                val intent = Intent(this, OutboundDetailActivity::class.java).apply {

                    putExtra("OUTBOUND_ID", invoice.id.toString())
                    putExtra("INVOICE_NUM", invoice.invorseNumber.toString())
                    putExtra("CUSTOMER_NAME", invoice.customerName)
                    putExtra("previousDebt", invoice.previousDebt)
                    putExtra("totalRemainder", invoice.totalRemainder)
                    putExtra("customerId", invoice.customerId)
                    putExtra("DATE", invoice.outboundDate.take(10).trim())
                    putExtra("PAID_AMOUNT", invoice.moneyResive.toString())
                }
                startActivity(intent)
            },
            onLongItemClick = {
                showUpdateDeleteMenu(this,recyclerView,it,{
                    openEditOutbound(it)
                },{
                    showDeleteConfirmationDialog(it)
                })
                return@OutboundAdapter true
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter


    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun displayInvoices(invoices: List<Outbound>) {



        setupRecyclerView(invoices)

    }


    private fun createTextView(txt: String): TextView {
        return TextView(this).apply {
            text = txt
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.black))
            textSize = 14f
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

}