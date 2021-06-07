package com.example.ringtoneapp

import android.app.Application
import com.example.ringtoneapp.di.components.AppComponent
import com.example.ringtoneapp.di.components.DaggerAppComponent
import com.example.ringtoneapp.di.module.AppModule

class App: Application() {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.builder().appModule(AppModule()).build()
    }

}