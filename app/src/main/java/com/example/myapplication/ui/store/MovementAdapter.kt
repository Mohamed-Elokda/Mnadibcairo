package com.example.myapplication.ui.store

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.domin.model.ItemMovement

class MovementAdapter : ListAdapter<ItemMovement, MovementAdapter.MovementViewHolder>(MovementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movement_row, parent, false)
        return MovementViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        val movement = getItem(position)
        holder.bind(movement)
    }

    class MovementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvQtyIn: TextView = itemView.findViewById(R.id.tvQtyIn)
        private val tvQtyOut: TextView = itemView.findViewById(R.id.tvQtyOut)
        private val tvRunningStock: TextView = itemView.findViewById(R.id.tvRunningStock)

        fun bind(item: ItemMovement) {
            // 1. التاريخ والبيان
            tvDate.text = item.date
            tvType.text = "${item.transactionType}\n#${item.documentNumber}"
            tvType.text = "${item.transactionType} (#${item.documentNumber})\nجهة: ${item.partyName}"
            // 2. الوارد (أخضر) - إخفاء الصفر
            if (item.qtyIn > 0) {
                tvQtyIn.text = item.qtyIn.toString()
                tvQtyIn.setTextColor(Color.parseColor("#388E3C"))
            } else {
                tvQtyIn.text = "-"
                tvQtyIn.setTextColor(Color.LTGRAY)
            }

            // 3. الصادر (أحمر) - إخفاء الصفر
            if (item.qtyOut > 0) {
                tvQtyOut.text = item.qtyOut.toString()
                tvQtyOut.setTextColor(Color.parseColor("#D32F2F"))
            } else {
                tvQtyOut.text = "-"

                tvQtyOut.setTextColor(Color.LTGRAY)
            }

            // 4. الرصيد التراكمي في المخزن
            tvRunningStock.text = item.runningStock.toString()

            // تمييز لون الرصيد بناءً على الكمية (تحذير لو نقص عن الصفر)
            if (item.runningStock < 0) {
                tvRunningStock.setTextColor(Color.RED)
            } else {
                tvRunningStock.setTextColor(Color.parseColor("#1976D2")) // أزرق براند
            }

            // 5. تلوين خلفية السطر بشكل تبادلي لسهولة القراءة
            if (adapterPosition % 2 == 0) {
                itemView.setBackgroundColor(Color.parseColor("#FFFFFF"))
            } else {
                itemView.setBackgroundColor(Color.parseColor("#F5F5F5"))
            }
        }
    }

    class MovementDiffCallback : DiffUtil.ItemCallback<ItemMovement>() {
        override fun areItemsTheSame(oldItem: ItemMovement, newItem: ItemMovement): Boolean {
            // المقارنة بالرقم المرجعي والتاريخ لضمان التحديث
            return oldItem.documentNumber == newItem.documentNumber &&
                    oldItem.transactionType == newItem.transactionType &&
                    oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: ItemMovement, newItem: ItemMovement): Boolean {
            return oldItem == newItem
        }
    }
}