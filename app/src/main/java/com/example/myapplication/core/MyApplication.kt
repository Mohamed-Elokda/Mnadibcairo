package com.example.myapplication.core

import android.app.Application
import com.example.myapplication.domin.repository.CustomerRepo

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleSync(context = this)

    }
}