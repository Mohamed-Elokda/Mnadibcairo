package com.example.myapplication.ui.returned

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.model.ReturnedWithNameModel

class ReturnedAdapter(private val onItemClick: (ReturnedWithNameModel) -> Unit,private val onLongClick: (ReturnedWithNameModel) -> Unit ):
    ListAdapter<ReturnedWithNameModel, ReturnedAdapter.ReturnedViewHolder>(ReturnedDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReturnedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_returned_invoice, parent, false)
        return ReturnedViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReturnedViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick)
    }

    inner class ReturnedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvReturnAmount: TextView = itemView.findViewById(R.id.tvReturnAmount)
        private val tvInvoiceId: TextView = itemView.findViewById(R.id.tvInvoiceId)
        private val tvReturnDate: TextView = itemView.findViewById(R.id.tvReturnDate)

        fun bind(item: ReturnedWithNameModel, onItemClick: (ReturnedWithNameModel) -> Unit) {
            tvCustomerName.text = item.customerName
            tvReturnAmount.text = String.format("%.2f ج.م", item.totalPrice)
            tvInvoiceId.text = "فاتورة أصلية: #${item.returnedModel.id}"
            tvReturnDate.text = item.returnedModel.returnedDate

            // عرض السبب إذا وجد، أو إخفاء الحقل إذا كان فارغاً


            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener { onLongClick(item)
                true }
        }
    }

    class ReturnedDiffCallback : DiffUtil.ItemCallback<ReturnedWithNameModel>() {
        override fun areItemsTheSame(oldItem: ReturnedWithNameModel, newItem: ReturnedWithNameModel) = oldItem.returnedModel.id == newItem.returnedModel.id
        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: ReturnedWithNameModel, newItem: ReturnedWithNameModel) = oldItem == newItem
    }
}