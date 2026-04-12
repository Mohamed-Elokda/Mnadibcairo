package com.example.myapplication.core

import net.objecthunter.exp4j.ExpressionBuilder

fun calculateResult(input: String): String {
    return try {
        // تنظيف النص من أي رموز غريبة (اختياري)
        val cleanInput = input.replace("×", "*").replace("÷", "/")

        // بناء المعادلة وحسابها
        val expression = ExpressionBuilder(cleanInput).build()
        val result = expression.evaluate()

        // إذا كان الناتج رقماً صحيحاً، اعرضه بدون فاصلة عشرية
        if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            result.toString()
        }
    } catch (e: Exception) {
        "خطأ" // في حال كانت المعادلة غير صحيحة مثل 5++5
    }
}