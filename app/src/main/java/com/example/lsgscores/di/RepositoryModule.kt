package com.example.lsgscores.di

import com.example.lsgscores.data.hole.HoleDao
import com.example.lsgscores.data.hole.HoleRepository
import com.example.lsgscores.data.holemode.HoleGameModeRepository
import com.example.lsgscores.data.media.MediaDao
import com.example.lsgscores.data.media.MediaRepository
import com.example.lsgscores.data.player.PlayerDao
import com.example.lsgscores.data.player.PlayerRepository
import com.example.lsgscores.data.scoring.ScoringModeRepository
import com.example.lsgscores.data.session.PlayedHoleDao
import com.example.lsgscores.data.session.PlayedHoleRepository
import com.example.lsgscores.data.session.PlayedHoleScoreDao
import com.example.lsgscores.data.session.PlayedHoleScoreRepository
import com.example.lsgscores.data.session.SessionDao
import com.example.lsgscores.data.session.SessionRepository
import com.example.lsgscores.data.session.TeamDao
import com.example.lsgscores.data.session.TeamRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePlayerRepository(playerDao: PlayerDao): PlayerRepository {
        return PlayerRepository(playerDao)
    }

    @Provides
    @Singleton
    fun provideHoleRepository(holeDao: HoleDao): HoleRepository {
        return HoleRepository(holeDao)
    }

    @Provides
    @Singleton
    fun provideTeamRepository(teamDao: TeamDao): TeamRepository {
        return TeamRepository(teamDao)
    }

    @Provides
    @Singleton
    fun provideMediaRepository(mediaDao: MediaDao): MediaRepository {
        return MediaRepository(mediaDao)
    }

    @Provides
    @Singleton
    fun providePlayedHoleRepository(
        playedHoleDao: PlayedHoleDao,
        playedHoleScoreDao: PlayedHoleScoreDao
    ): PlayedHoleRepository {
        return PlayedHoleRepository(playedHoleDao, playedHoleScoreDao)
    }

    @Provides
    @Singleton
    fun providePlayedHoleScoreRepository(playedHoleScoreDao: PlayedHoleScoreDao): PlayedHoleScoreRepository {
        return PlayedHoleScoreRepository(playedHoleScoreDao)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        teamDao: TeamDao,
        playedHoleDao: PlayedHoleDao,
        playedHoleScoreDao: PlayedHoleScoreDao
    ): SessionRepository {
        return SessionRepository(sessionDao, teamDao, playedHoleDao, playedHoleScoreDao)
    }

    @Provides
    @Singleton
    fun provideScoringModeRepository(): ScoringModeRepository {
        return ScoringModeRepository()
    }

    @Provides
    @Singleton
    fun provideHoleGameModeRepository(): HoleGameModeRepository {
        return HoleGameModeRepository()
    }
}