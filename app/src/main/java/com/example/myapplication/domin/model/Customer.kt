package com.example.myapplication.domin.model



data class Customer(
    val id: Int,
    val customerName: String,
    val userId: String,
    val CustomerNum: Int,

    val customerDebt: Double

){
    override fun toString(): String {
      val words=  customerName.split("\\s".toRegex())

        val shortName = if (words.size > 3) {
            "${words[0]} ${words[1]} ${words[2]} ${words[3]}..."
        } else {
            customerName // إذا كان كلمة واحدة أو كلمتين أصلاً يظهر كما هو
        }
        return "${shortName} (الحساب: $customerDebt)"
    }
}

