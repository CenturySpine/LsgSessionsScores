package fr.centuryspine.lsgscores.di

import fr.centuryspine.lsgscores.data.hole.HoleDao
import fr.centuryspine.lsgscores.data.hole.HoleRepository
import fr.centuryspine.lsgscores.data.holemode.HoleGameModeRepository
import fr.centuryspine.lsgscores.data.player.PlayerDao
import fr.centuryspine.lsgscores.data.player.PlayerRepository
import fr.centuryspine.lsgscores.data.scoring.ScoringModeRepository
import fr.centuryspine.lsgscores.data.session.PlayedHoleDao
import fr.centuryspine.lsgscores.data.session.PlayedHoleRepository
import fr.centuryspine.lsgscores.data.session.PlayedHoleScoreDao
import fr.centuryspine.lsgscores.data.session.PlayedHoleScoreRepository
import fr.centuryspine.lsgscores.data.session.SessionDao
import fr.centuryspine.lsgscores.data.session.SessionRepository
import fr.centuryspine.lsgscores.data.session.TeamDao
import fr.centuryspine.lsgscores.data.session.TeamRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import fr.centuryspine.lsgscores.data.gamezone.GameZoneRepository
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import fr.centuryspine.lsgscores.utils.SupabaseStorageHelper
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePlayerRepository(
        playerDao: PlayerDao,
        appPreferences: AppPreferences,
        storageHelper: SupabaseStorageHelper,
        imageCacheManager: fr.centuryspine.lsgscores.utils.ImageCacheManager
    ): PlayerRepository {
        return PlayerRepository(playerDao, appPreferences, storageHelper, imageCacheManager)
    }

    @Provides
    @Singleton
    fun provideHoleRepository(
        holeDao: HoleDao,
        gameZoneDao: GameZoneDao,
        appPreferences: AppPreferences,
        storageHelper: SupabaseStorageHelper,
        imageCacheManager: fr.centuryspine.lsgscores.utils.ImageCacheManager
    ): HoleRepository {
        return HoleRepository(holeDao, gameZoneDao, appPreferences, storageHelper, imageCacheManager)
    }

    @Provides
    @Singleton
    fun provideTeamRepository(teamDao: TeamDao): TeamRepository {
        return TeamRepository(teamDao)
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
        playedHoleScoreDao: PlayedHoleScoreDao,
        gameZoneDao: GameZoneDao  // Add dependency
    ): SessionRepository {
        return SessionRepository(sessionDao, teamDao, playedHoleDao, playedHoleScoreDao, gameZoneDao)
    }

    @Provides
    @Singleton
    fun provideScoringModeRepository(scoringModeDao: fr.centuryspine.lsgscores.data.scoring.ScoringModeDao): ScoringModeRepository {
        return ScoringModeRepository(scoringModeDao)
    }

    @Provides
    @Singleton
    fun provideHoleGameModeRepository(): HoleGameModeRepository {
        return HoleGameModeRepository()
    }

    @Provides
    @Singleton
    fun provideGameZoneRepository(
        gameZoneDao: GameZoneDao,
        holeDao: HoleDao,
        sessionDao: SessionDao,
        appPreferences: AppPreferences
    ): GameZoneRepository {
        return GameZoneRepository(gameZoneDao, holeDao, sessionDao, appPreferences)
    }
}