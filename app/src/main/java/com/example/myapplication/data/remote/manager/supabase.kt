package com.example.myapplication.data.remote.manager

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

@OptIn(SupabaseInternal::class)
val supabase = createSupabaseClient(
    supabaseUrl = "https://wktoywqvndtxozxbrmha.supabase.co",
    supabaseKey = "sb_publishable_EA11pxS0e2KdoyxhXbWU6A_3QUN5Uww"
) {
    install(Postgrest)
    install(Storage) // ستحتاجه لرفع الصور لاحقاً
// الإعداد الصحيح للوقت في النسخ الحديثة
    httpConfig {
        // وقت تنفيذ الطلب بالكامل
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 30000L  // 30 ثانية
            connectTimeoutMillis = 30000L  // 30 ثانية
            socketTimeoutMillis = 30000L   // 30 ثانية
        }
    }
}