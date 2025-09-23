package com.hanif.cmppicker

import android.app.Application
import android.content.Context

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContext.init(this)
    }
}

object AndroidContext {
    lateinit var applicationContext: Context
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}