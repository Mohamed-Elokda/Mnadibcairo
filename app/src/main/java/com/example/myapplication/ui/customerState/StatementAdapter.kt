package com.example.myapplication.ui.customerState

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.domin.model.StatementTransaction

class StatementAdapter : ListAdapter<StatementTransaction, StatementAdapter.StatementViewHolder>(StatementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_statement_row, parent, false)
        return StatementViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatementViewHolder, position: Int) {
        val transaction = getItem(position)

        // جلب العنصر السابق للمقارنة (لإخفاء التكرار)
        val previousTransaction = if (position > 0) getItem(position - 1) else null

        holder.bind(transaction, previousTransaction)
    }

    class StatementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val tvDebit: TextView = itemView.findViewById(R.id.tvDebit)
        private val tvCredit: TextView = itemView.findViewById(R.id.tvCredit)
        private val tvBalance: TextView = itemView.findViewById(R.id.tvBalance)

        fun bind(item: StatementTransaction, previousItem: StatementTransaction?) {
            // منطق إخفاء التاريخ والوصف المتكرر
            if (previousItem != null && previousItem.description == item.description && previousItem.date == item.date) {
                tvDate.visibility = View.INVISIBLE
                tvDescription.visibility = View.INVISIBLE
            } else {
                tvDate.visibility = View.VISIBLE
                tvDescription.visibility = View.VISIBLE
                tvDate.text = item.date
                tvDescription.text = item.description
            }

            tvItemName.text = item.itemName
            tvQty.text = if (item.quantity > 0) item.quantity.toString() else ""
            tvDebit.text = if (item.amountIn > 0) String.format("%.2f", item.amountIn) else ""
            tvCredit.text = if (item.amountOut > 0) String.format("%.2f", item.amountOut) else ""
            tvBalance.text = String.format("%.2f", item.runningBalance)

            // تلوين الرصيد
            if (item.runningBalance > 0) {
                tvBalance.setTextColor(Color.parseColor("#D32F2F"))
            } else {
                tvBalance.setTextColor(Color.parseColor("#388E3C"))
            }
        }
    }
    class StatementDiffCallback : DiffUtil.ItemCallback<StatementTransaction>() {
        override fun areItemsTheSame(oldItem: StatementTransaction, newItem: StatementTransaction): Boolean {
            return oldItem.date == newItem.date &&
                    oldItem.description == newItem.description &&
                    oldItem.itemName == newItem.itemName &&
                    oldItem.runningBalance == newItem.runningBalance // أضف هذا السطر
        }
        override fun areContentsTheSame(oldItem: StatementTransaction, newItem: StatementTransaction): Boolean {
            return oldItem == newItem
        }
    }
}