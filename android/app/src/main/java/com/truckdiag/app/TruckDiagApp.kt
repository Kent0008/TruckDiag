package com.truckdiag.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.truckdiag.app.data.remote.ApiClient

class TruckDiagApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
        }
        ApiClient.init(this)
    }
}
