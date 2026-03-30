package com.example.myapplication.domin.model


data class Stock(

    val id: Int =0,
    val ItemId: Int=0,
    val userId: String,
    val InitAmount: Int=0,
    val CurrentAmount: Int=0,
    val fristDate:String="",
    val itemName: String = "", // أضفنا هذا الحقل
    val isSynced: Boolean
){
    override fun toString(): String {
        return "$itemName (الكمية: $CurrentAmount)"
    }
}