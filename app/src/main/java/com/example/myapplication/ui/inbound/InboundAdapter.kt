package com.example.myapplication.ui.inbound

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.core.formatToEnglishDigits
import com.example.myapplication.domin.model.Inbound
import java.util.*

class InboundAdapter(
    private var inboundList: List<Inbound>,
    private val onItemClick: (Inbound) -> Unit ,
    private val onLongItemClick: (Inbound) -> Boolean ,
) : RecyclerView.Adapter<InboundAdapter.InboundViewHolder>() {

    class InboundViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInvoiceNum: TextView = view.findViewById(R.id.tvInvoiceNum)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSupplier: TextView = view.findViewById(R.id.tvSupplierName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboundViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inbound_card, parent, false)
        return InboundViewHolder(view)
    }

    override fun onBindViewHolder(holder: InboundViewHolder, position: Int) {
        val inbound = inboundList[position]

        // ربط البيانات بالواجهة
        holder.tvInvoiceNum.text = "#${inbound.invorseNum}"
        holder.tvDate.text = formatToReadableDate(inbound.inboundDate) // دالة مساعدة
        holder.tvSupplier.text = inbound.suppliedName

        // معالجة الضغط على الفاتورة
        holder.itemView.setOnClickListener {
            onItemClick(inbound)
        }
        holder.itemView.setOnLongClickListener {
            onLongItemClick(inbound)
        }
    }

    override fun getItemCount() = inboundList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Inbound>) {
        inboundList = newList
        notifyDataSetChanged()
    }

    // دالة مساعدة لتحويل التاريخ لشكل أجمل (اختياري)
    private fun formatToReadableDate(dateStr: String): String {


        // 2. تنظيف تاريخ الفاتورة وتحويل أرقامه للإنجليزية
        val rawDate = dateStr.substringBefore("T").trim()
        val cleanedInvoiceDate = formatToEnglishDigits(rawDate)
        // يمكنك هنا استخدام SimpleDateFormat لتحويل yyyy-MM-dd إلى yyyy/MM/dd أو أي شكل تحبه
        return cleanedInvoiceDate.replace("-", "/")
    }
}