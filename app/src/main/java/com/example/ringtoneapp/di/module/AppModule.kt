package com.example.ringtoneapp.di.module

import com.example.ringtoneapp.player.ISimplePlayer
import com.example.ringtoneapp.player.SimplePlayer
import com.example.ringtoneapp.presenters.IMainPresenter
import com.example.ringtoneapp.presenters.IPlayPresenter
import com.example.ringtoneapp.presenters.MainPresenter
import com.example.ringtoneapp.presenters.PlayPresenter
import com.example.ringtoneapp.trimmer.ITrimmer
import com.example.ringtoneapp.trimmer.Trimmer
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {

    @Provides
    fun providePlayer(): ISimplePlayer {
        return SimplePlayer()
    }

    @Provides
    @Singleton
    fun provideTrimmer(): ITrimmer {
        return Trimmer()
    }

    @Provides
    @Singleton
    fun providePlayPresenter(player: ISimplePlayer): IPlayPresenter {
        return PlayPresenter(player)
    }

    @Provides
    @Singleton
    fun provideMainPresenter(player: ISimplePlayer, trimmer: ITrimmer): IMainPresenter {
        return MainPresenter(player, trimmer)
    }
}