package fr.centuryspine.lsgscores.di

import android.content.Context
import androidx.room.Room
import fr.centuryspine.lsgscores.data.AppDatabase

import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import fr.centuryspine.lsgscores.data.hole.HoleDao
import fr.centuryspine.lsgscores.data.media.MediaDao
import fr.centuryspine.lsgscores.data.player.PlayerDao
import fr.centuryspine.lsgscores.data.scoring.ScoringModeDao
import fr.centuryspine.lsgscores.data.session.PlayedHoleDao
import fr.centuryspine.lsgscores.data.session.PlayedHoleScoreDao
import fr.centuryspine.lsgscores.data.session.SessionDao
import fr.centuryspine.lsgscores.data.session.TeamDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import fr.centuryspine.lsgscores.data.Migrations


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Replace the existing provideAppDatabase function with:

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "streetgolf-db"
        )
            .addMigrations(*Migrations.ALL_MIGRATIONS)
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