package edu.amrita.amritacafe

import android.app.Application
import android.content.Context

class AmritaCafeApp : Application() {

    companion object {
        lateinit var instance: AmritaCafeApp
            private set

        val appContext: Context
            get() = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
