package com.example.myapplication.core


 fun formatToEnglishDigits(input: String): String {
        var result = input
        val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val englishChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        for (i in 0..9) {
            result = result.replace(arabicChars[i], englishChars[i])
        }
        return result
    }
