package com.example.myapplication

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountTransaction(
    @SerialName("id")
    val id: String? = null, // يُترك null عند الإضافة ليقوم السيرفر بتوليده

    @SerialName("stakeholder_id")
    val stakeholder_id: String?, // معرف المورد أو العميل (UUID)

    @SerialName("amount")
    val amount: Double, // قيمة العملية

    @SerialName("transaction_type")
    val transaction_type: String, // النوع: 'inbound', 'outbound', 'payment', 'receipt'

    @SerialName("invoice_id")
    val invoice_id: String? = null, // ربط العملية بفاتورة معينة إن وجد

    @SerialName("note")
    val note: String? = null, // وصف للعملية (مثل: فاتورة شراء رقم 10)

    @SerialName("warehouse_name")
    val warehouse_name: String? = null,

    @SerialName("company_id")
    val company_id: String? = null, // معرف الشركة أو المستخدم (للأمان RLS)

    @SerialName("created_at")
    val created_at: String? = null // تاريخ العملية
)