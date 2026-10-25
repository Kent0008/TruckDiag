package com.truckdiag.app

import android.app.Application
import com.truckdiag.app.data.remote.ApiClient

class TruckDiagApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}
