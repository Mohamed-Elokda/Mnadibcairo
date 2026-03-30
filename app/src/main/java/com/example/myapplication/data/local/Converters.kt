package com.example.myapplication.data.local



import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class Converters {
    // تنسيق ثابت بالأرقام الإنجليزية (yyyy-MM-dd)
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let {
            // عشان نضمن إنه يقرأ أي أرقام قديمة كانت متسيفة غلط (عربي)
            val cleanValue = it.toEnglishDigits()
            LocalDate.parse(cleanValue, formatter)
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        // التحويل لنص دايماً هيكون بالأرقام الإنجليزية
        return date?.format(formatter)
    }

    // دالة مساعدة لتحويل الأرقام العربية لإنجليزي (لزيادة الأمان)
    private fun String.toEnglishDigits(): String {
        var result = this
        val arabicDigits = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
        for (i in 0..9) {
            result = result.replace(arabicDigits[i], i.toString())
        }
        return result
    }
}