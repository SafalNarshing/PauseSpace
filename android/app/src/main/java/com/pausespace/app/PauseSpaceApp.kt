package com.pausespace.app

import android.app.Application
import com.pausespace.app.data.InterventionLog
import com.pausespace.app.data.Store

class PauseSpaceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        InterventionLog.init(this)
    }
}
