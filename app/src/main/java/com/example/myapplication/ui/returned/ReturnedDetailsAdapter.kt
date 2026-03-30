package com.example.myapplication.ui.returned

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.domin.model.ReturnedDetailsModel

class ReturnedDetailsAdapter : ListAdapter<ReturnedDetailsModel, ReturnedDetailsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_details_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName = view.findViewById<TextView>(R.id.tvRowItemName)
        private val tvQty = view.findViewById<TextView>(R.id.tvRowQty)
        private val tvPrice = view.findViewById<TextView>(R.id.tvRowPrice)
        private val tvTotal = view.findViewById<TextView>(R.id.tvRowTotal)

        fun bind(item: ReturnedDetailsModel) {
            tvName.text = item.itemName
            tvQty.text = item.amount.toString()
            tvPrice.text = String.format("%.2f", item.price)
            tvTotal.text = String.format("%.2f", item.amount * item.price)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ReturnedDetailsModel>() {
        override fun areItemsTheSame(old: ReturnedDetailsModel, new: ReturnedDetailsModel) = old.id == new.id
        override fun areContentsTheSame(old: ReturnedDetailsModel, new: ReturnedDetailsModel) = old == new
    }
}