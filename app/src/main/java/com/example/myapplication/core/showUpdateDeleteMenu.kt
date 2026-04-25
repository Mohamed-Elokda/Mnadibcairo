package com.example.myapplication.core

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.example.myapplication.domin.model.Outbound
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SuspiciousIndentation")
public fun showUpdateDeleteMenu(context: Context, view: View, invoice: Outbound, editClick:(invoice: Outbound)-> Unit, deleteClick:(invoice: Outbound)-> Unit) {
    val popup = PopupMenu(context, view)

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    val currentDateStr = sdf.format(Date())

    val rawDate = invoice.outboundDate.take(10).trim()
    val cleanedInvoiceDate = formatToEnglishDigits(rawDate)

//    if (currentDateStr == cleanedInvoiceDate) {
        popup.menu.add("تعديل")
        popup.menu.add("حذف")
//    } else {
//        val item = popup.menu.add("لا يمكن التعديل/الحذف (تاريخ قديم)")
//        item.isEnabled = false
//    }

    popup.setOnMenuItemClickListener { item ->
        when (item.title) {
            "تعديل" -> editClick(invoice)
            "حذف" -> deleteClick(invoice)
        }
        true
    }
    popup.show()
}
