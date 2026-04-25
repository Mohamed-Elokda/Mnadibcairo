package com.example.myapplication.ui.outbound


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.core.formatToEnglishDigits
import com.example.myapplication.domin.model.Inbound
import com.example.myapplication.domin.model.Outbound

class OutboundAdapter(
    private var inboundList: List<Outbound>,
    private val onItemClick: (Outbound) -> Unit,
    private val onLongItemClick: (Outbound) -> Boolean ,

    ) : RecyclerView.Adapter<OutboundAdapter.OutboundViewHolder>() {

    class OutboundViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInvoiceNum: TextView = view.findViewById(R.id.tvInvoiceNum)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSupplier: TextView = view.findViewById(R.id.tvSupplierName)
        val tvTotalAmount: TextView = view.findViewById(R.id.tvTotalAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutboundViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_outbound_card, parent, false)
        return OutboundViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutboundViewHolder, position: Int) {
        val inbound = inboundList[position]

        // ربط البيانات بالواجهة
        holder.tvInvoiceNum.text = "#${inbound.invorseNumber}"
        holder.tvDate.text = formatToReadableDate(inbound.outboundDate) // دالة مساعدة
        holder.tvSupplier.text = inbound.customerName
        holder.tvTotalAmount.text = "${String.format("%.2f", inbound.totalRemainder)} ج.م"

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
    fun updateList(newList: List<Outbound>) {
        inboundList = newList
        notifyDataSetChanged()
    }

    private fun formatToReadableDate(dateStr: String): String {
        val rawDate = dateStr.substringBefore("T").trim()
        val cleanedInvoiceDate = formatToEnglishDigits(rawDate)
        return cleanedInvoiceDate.replace("-", "/")
    }
}