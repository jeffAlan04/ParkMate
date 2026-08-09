package com.example.parkmate

import android.app.Application
import com.example.parkmate.di.AppContainer

class ParkMateApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}