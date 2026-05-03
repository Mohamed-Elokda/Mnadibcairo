package com.example.myapplication.ui.payment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.domin.model.PaymentItem // تأكد من المسار الصحيح للموديل

class PaymentsAdapter(private val onItemClick: (PaymentItem) -> Unit) :
    ListAdapter<PaymentItem, PaymentsAdapter.PaymentViewHolder>(PaymentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payment, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick)
    }

    class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvPaymentType: TextView = itemView.findViewById(R.id.tvPaymentType)

        fun bind(item: PaymentItem, onItemClick: (PaymentItem) -> Unit) {
            tvCustomerName.text = item.customerName
            tvAmount.text = "${item.amount} ج.م"
            tvDate.text = item.date
            tvPaymentType.text = item.paymentType
            tvNote.text=item.notes
            // تغيير لون الخلفية والنص بناءً على نوع العملية
            val context = itemView.context
            when (item.paymentType) {
                "فودافون كاش" -> {
                    tvPaymentType.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    tvPaymentType.setBackgroundResource(R.drawable.bg_red_rounded) // سأعطيك كود الـ drawable بالأسفل
                }
                "إنستا باي" -> {
                    tvPaymentType.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    tvPaymentType.setBackgroundResource(R.drawable.bg_purple_rounded)
                }
                "نقدي" -> {
                    tvPaymentType.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    tvPaymentType.setBackgroundResource(R.drawable.bg_green_rounded)
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    class PaymentDiffCallback : DiffUtil.ItemCallback<PaymentItem>() {
        override fun areItemsTheSame(oldItem: PaymentItem, newItem: PaymentItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PaymentItem, newItem: PaymentItem) = oldItem == newItem
    }
}