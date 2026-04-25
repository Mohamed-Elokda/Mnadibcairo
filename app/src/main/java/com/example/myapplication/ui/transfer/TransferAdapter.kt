package com.example.myapplication.ui.transfer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.local.entity.TransferEntity
import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferWithStoreName

class TransferAdapter (private val onItemClick: (TransferWithStoreName) -> Unit,private val onLongClick: (TransferWithStoreName) -> Boolean): RecyclerView.Adapter<TransferAdapter.ViewHolder>() {

    private var list = listOf<TransferWithStoreName>()

    fun submitList(newList: List<TransferWithStoreName>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
       val tvNum: TextView = view.findViewById(R.id.tvNote) // تأكد من الـ IDs في XML
        val tvDate: TextView = view.findViewById(R.id.tvDate)
//        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
       holder.tvNum.text = "مناقلة رقم: ${item.transferNum}"
        holder.tvDate.text = item.date
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
        }
//        holder.tvStatus.text = if (item.isSynced) "مُزامن" else "قيد الانتظار"
    }

    override fun getItemCount() = list.size
}