package com.example.lsgscores.di

import android.content.Context
import androidx.room.Room
import com.example.lsgscores.data.AppDatabase

import com.example.lsgscores.data.gamezone.GameZoneDao
import com.example.lsgscores.data.hole.HoleDao
import com.example.lsgscores.data.media.MediaDao
import com.example.lsgscores.data.player.PlayerDao
import com.example.lsgscores.data.scoring.ScoringModeDao
import com.example.lsgscores.data.session.PlayedHoleDao
import com.example.lsgscores.data.session.PlayedHoleScoreDao
import com.example.lsgscores.data.session.SessionDao
import com.example.lsgscores.data.session.TeamDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "streetgolf-db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePlayerDao(database: AppDatabase): PlayerDao = database.userDao()

    @Provides
    fun provideHoleDao(database: AppDatabase): HoleDao = database.holeDao()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideScoringModeDao(database: AppDatabase): ScoringModeDao = database.scoringModeDao()

    @Provides
    fun provideMediaDao(database: AppDatabase): MediaDao = database.mediaDao()

    @Provides
    fun provideTeamDao(database: AppDatabase): TeamDao = database.teamDao()

    @Provides
    fun providePlayedHoleDao(database: AppDatabase): PlayedHoleDao = database.playedHoleDao()

    @Provides
    fun providePlayedHoleScoreDao(database: AppDatabase): PlayedHoleScoreDao = database.playedHoleScoreDao()

    @Provides
    fun provideGameZoneDao(database: AppDatabase): GameZoneDao = database.gameZoneDao()
}