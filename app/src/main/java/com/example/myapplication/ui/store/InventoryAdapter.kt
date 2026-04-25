package com.example.myapplication.ui.store

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.domin.model.Stock
import androidx.core.graphics.toColorInt

class InventoryAdapter(private var items: List<Stock>,private val onItemClick:(Stock)-> Unit) :
    RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    class InventoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvProductName)
        val tvStock: TextView = view.findViewById(R.id.tvStockCount)
        val tvCategory: TextView = view.findViewById(R.id.tvProductCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory_card, parent, false)
        return InventoryViewHolder(view)
    }



    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.itemName
        holder.tvStock.text = item.InitAmount.toString()
holder.itemView.setOnClickListener {
    onItemClick(item)
}
        // تغيير لون الرقم لو الكمية قليلة (لمسة احترافية)
        if (item.CurrentAmount < 10) {
            holder.tvStock.setTextColor(Color.RED)
        } else {
            holder.tvStock.setTextColor("#6200EE".toColorInt())
        }
    }

    override fun getItemCount() = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Stock>) {
        items = newList
        notifyDataSetChanged()
    }
}