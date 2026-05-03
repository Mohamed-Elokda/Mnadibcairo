package com.example.myapplication.ui.inbound

import android.R.attr.padding
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.data.formatToEnglish
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.domin.model.Inbound
import com.example.myapplication.domin.useCase.inboundUseCases.AddInboundUseCase
import com.example.myapplication.domin.useCase.inboundUseCases.GetInboundDetailsUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class InboundActivity : AppCompatActivity() {


    // استخدم ViewModelProvider للحصول على نسخة من الـ ViewModel
    // 1. تجهيز المتطلبات (يفضل استخدام Dependency Injection مستقبلاً مثل Hilt)
    private val viewModel: InboundViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbound)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainadI)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnAddInbound = findViewById<Button>(R.id.btnAddInbound)
        val etSearch = findViewById<EditText>(R.id.etSearch)

        btnBack.setOnClickListener { finish() }
        scheduleSync(context = this)

        viewModel.getInbounds(Prefs.getUserId(this)!!).observe(this) { inboundList ->
            try {
                displayInboundData(inboundList)
            } catch (ex: Exception) {
                Toast.makeText(this@InboundActivity, ex.message, Toast.LENGTH_SHORT).show()
            }

        }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                viewModel.getInbounds(Prefs.getUserId(this@InboundActivity)!!)
                    .observe(this@InboundActivity) { list ->
                        val filteredList = list.filter {
                            it.invorseNum.toString().contains(query) || it.suppliedName.toString()
                                .contains(query)
                        }
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


        setupRecyclerView(inboundList)

    }



    @SuppressLint("SuspiciousIndentation")
    private fun showUpdateDeleteMenu(view: View, inbound: Inbound) {
        val popup = PopupMenu(this, view)

        // 1. تجهيز تاريخ اليوم بالإنجليزية
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val currentDateStr = sdf.format(Date())

        // 2. تنظيف تاريخ الفاتورة وتحويل أرقامه للإنجليزية
        val rawDate = inbound.inboundDate.substringBefore("T").trim()
        val cleanedInvoiceDate = formatToEnglishDigits(rawDate)

        // 3. التحقق من الشرط (نفس اليوم)

            popup.menu.add("تعديل")
            popup.menu.add("حذف")


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

    private fun setupRecyclerView(inboundList: List<Inbound>) {
        val recyclerView = findViewById<RecyclerView>(R.id.rvInbound)

        val adapter = InboundAdapter(
            inboundList,
            onItemClick = { selectedInbound ->


                val intent = Intent(this, InboundDetailActivity::class.java).apply {
                    putExtra("INBOUND_ID", selectedInbound.id)

                    putExtra("INVOICE_NUM", selectedInbound.invorseNum.toString())
                    putExtra("SUPPLIER", selectedInbound.suppliedName)
                    putExtra("DATE", selectedInbound.inboundDate)


                }
                startActivity(intent)
            },
            onLongItemClick = {

                showUpdateDeleteMenu(recyclerView, it)
                return@InboundAdapter true
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter


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
            putExtra("INBOUND_ID", inbound.id)
            putExtra("INVOICE_NUM", inbound.invorseNum.toString())
            putExtra("SUPPLIED_ID", inbound.fromSppliedId)
            putExtra("SUPPLIER", inbound.suppliedName)
            // يمكنك تمرير باقي البيانات حسب الحاجة
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmationDialog(inbound: Inbound) {
        AlertDialog.Builder(this)
            .setTitle("تأكيد الحذف")
            .setMessage("هل أنت متأكد من حذف الفاتورة رقم ${inbound.invorseNum}؟")
            .setPositiveButton("نعم") { _, _ ->
                viewModel.deleteInboundWithDetails(inbound)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }


}





