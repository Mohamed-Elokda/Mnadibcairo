package com.example.myapplication.data.repository

import com.example.myapplication.domin.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

class UserRepository @Inject constructor  (private val supabase: SupabaseClient) {

    suspend fun signInWithUsername(username: String, password: String): Result<UserProfile> {
        return try {
            // 1. البحث عن الإيميل بواسطة اليوزر نيم
            val profile = supabase.from("users_profiles")
                .select { filter { eq("username", username)
                    eq("password", password) } }
                .decodeSingleOrNull<UserProfile>()

            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(Exception("اسم المستخدم أو كلمة المرور غير صحيحة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}