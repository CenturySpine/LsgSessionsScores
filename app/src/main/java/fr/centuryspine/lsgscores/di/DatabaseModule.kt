package fr.centuryspine.lsgscores.di


import fr.centuryspine.lsgscores.data.authuser.CurrentUserProvider
import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import fr.centuryspine.lsgscores.data.hole.HoleDao
import fr.centuryspine.lsgscores.data.player.PlayerDao
import fr.centuryspine.lsgscores.data.scoring.ScoringModeDao
import fr.centuryspine.lsgscores.data.session.PlayedHoleDao
import fr.centuryspine.lsgscores.data.session.PlayedHoleScoreDao
import fr.centuryspine.lsgscores.data.session.SessionDao
import fr.centuryspine.lsgscores.data.session.TeamDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.centuryspine.lsgscores.data.city.CityDao
import io.github.jan.supabase.SupabaseClient


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {


    @Provides
    fun providePlayerDao(supabase: SupabaseClient, currentUserProvider: CurrentUserProvider): PlayerDao =
        fr.centuryspine.lsgscores.data.player.PlayerDaoSupabase(supabase, currentUserProvider)

    @Provides
    fun provideHoleDao(supabase: SupabaseClient, currentUserProvider: CurrentUserProvider): HoleDao =
        fr.centuryspine.lsgscores.data.hole.HoleDaoSupabase(supabase, currentUserProvider)

    @Provides
    fun provideSessionDao(supabase: SupabaseClient, currentUserProvider: CurrentUserProvider): SessionDao =
        fr.centuryspine.lsgscores.data.session.SessionDaoSupabase(supabase, currentUserProvider)

    @Provides
    fun provideScoringModeDao(supabase: SupabaseClient): ScoringModeDao =
        fr.centuryspine.lsgscores.data.scoring.ScoringModeDaoSupabase(supabase)


    @Provides
    fun provideTeamDao(supabase: SupabaseClient, currentUserProvider: CurrentUserProvider): TeamDao =
        fr.centuryspine.lsgscores.data.session.TeamDaoSupabase(supabase, currentUserProvider)

    @Provides
    fun providePlayedHoleDao(supabase: SupabaseClient, currentUserProvider: CurrentUserProvider): PlayedHoleDao =
        fr.centuryspine.lsgscores.data.session.PlayedHoleDaoSupabase(supabase, currentUserProvider)

    @Provides
    fun providePlayedHoleScoreDao(
        supabase: SupabaseClient,
        currentUserProvider: CurrentUserProvider
    ): PlayedHoleScoreDao =
        fr.centuryspine.lsgscores.data.session.PlayedHoleScoreDaoSupabase(supabase, currentUserProvider)

    @Provides
    fun provideGameZoneDao(supabase: SupabaseClient, currentUserProvider: CurrentUserProvider): GameZoneDao =
        fr.centuryspine.lsgscores.data.gamezone.GameZoneDaoSupabase(supabase, currentUserProvider)

    @Provides
    fun provideCityDao(supabase: SupabaseClient): CityDao =
        fr.centuryspine.lsgscores.data.city.CityDaoSupabase(supabase)
}