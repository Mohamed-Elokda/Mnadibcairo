package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityStatementBinding
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class StatementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatementBinding
    private var transactionList = listOf<AccountTransaction>()
    private var customerId: String? = null
    private var customerName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // استقبال بيانات العميل من الـ Intent
        customerId = intent.getStringExtra("CUSTOMER_ID")
        customerName = intent.getStringExtra("CUSTOMER_NAME")

        binding.tvCustomerName.text = customerName

        setupRecyclerView()


        binding.btnExportPdf.setOnClickListener {
            // هنا استدعاء دالة الـ PDF التي شرحناها سابقاً
          //  exportToPDF(customerName ?: "Client", transactionList)
        }
    }
    private fun showInvoiceDetails(transaction: AccountTransaction) {
        if (transaction.invoice_id == null) return // إذا لم تكن فاتورة (مثل رصيد افتتاحي) لا تفعل شيء


    }

    private fun showItemsDialog(items: List<InvoiceItemDetail>) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_invoice_details, null)
        val table = view.findViewById<TableLayout>(R.id.tableDetails)

        for (item in items) {
            val row = TableRow(this).apply { setPadding(0, 10, 0, 10) }

            row.addView(TextView(this).apply { text = item.item_name })
            row.addView(TextView(this).apply {
                text = item.quantity.toString()
                gravity = android.view.Gravity.CENTER
            })
            row.addView(TextView(this).apply {
                text = String.format("%.2f", item.unit_price)
                gravity = android.view.Gravity.CENTER
            })

            table.addView(row)
        }

        view.findViewById<Button>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(view)
        dialog.show()
    }



    private fun setupRecyclerView() {
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
    }
}