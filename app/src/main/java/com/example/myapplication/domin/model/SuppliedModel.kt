package com.example.myapplication.domin.model



data class SuppliedModel (
    val id:Int,
    val num: String,
    val suppliedName: String,
){
    override fun toString(): String {
        return suppliedName
    }
}