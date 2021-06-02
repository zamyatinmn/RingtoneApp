package com.example.ringtoneapp.di.components

import com.example.ringtoneapp.ui.MainActivity
import com.example.ringtoneapp.ui.PlayActivity
import com.example.ringtoneapp.di.module.AppModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(playActivity: PlayActivity)
}