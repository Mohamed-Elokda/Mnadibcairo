package com.example.myapplication.data.local.entitdata


class FullInboundExportModel(
    val invorseNum: String,  // رقم الفاتورة
    val sppliedId: String,   // اسم أو معرف المورد
    val inboundDate: String, // التاريخ
    val itemName: String,    // اسم الصنف من جدول التفاصيل
    val quantity: Double     // الكمية من جدول التفاصيل
)
