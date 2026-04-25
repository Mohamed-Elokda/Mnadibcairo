package com.example.myapplication.ui.customerState

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.domin.model.CustomerModel // تأكد من مسار الموديل عندك

class CustomerAdapter(
    private var customerModels: List<CustomerModel>,
    private val onItemClick: (CustomerModel) -> Unit // عشان لو دوست على العميل تفتح كشف حسابه
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    class CustomerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCustomerName)
        val tvDebt: TextView = view.findViewById(R.id.tvCustomerDebt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_card, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = customerModels[position]
        holder.tvName.text = customer.customerName

        // عرض المديونية بشكل شيك
        holder.tvDebt.text = "${String.format("%.2f", customer.customerDebt)} ج.م"

        // لو المديونية 0 خلي اللون أخضر، لو أكتر خليها أحمر (لمسة ذكية)
        if (customer.customerDebt <= 0) {
            holder.tvDebt.setTextColor(android.graphics.Color.parseColor("#2E7D32")) // أخضر
        } else {
            holder.tvDebt.setTextColor(android.graphics.Color.parseColor("#D32F2F")) // أحمر
        }

        holder.itemView.setOnClickListener { onItemClick(customer) }
    }

    override fun getItemCount() = customerModels.size

    // دالة التحديث المهمة جداً لعملية البحث
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<CustomerModel>) {
        customerModels = newList
        notifyDataSetChanged()
    }
}