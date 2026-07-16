package com.buspay.app

import android.app.Application
import android.content.Context

class BusPayApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguageManager.localizedContext(base))
    }
}
