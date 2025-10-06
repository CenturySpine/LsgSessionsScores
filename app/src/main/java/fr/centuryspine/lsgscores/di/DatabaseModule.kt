package fr.centuryspine.lsgscores.di


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
    fun providePlayerDao(supabase: SupabaseClient): PlayerDao = fr.centuryspine.lsgscores.data.player.PlayerDaoSupabase(supabase)

    @Provides
    fun provideHoleDao(supabase: SupabaseClient): HoleDao = fr.centuryspine.lsgscores.data.hole.HoleDaoSupabase(supabase)

    @Provides
    fun provideSessionDao(supabase: SupabaseClient): SessionDao = fr.centuryspine.lsgscores.data.session.SessionDaoSupabase(supabase)

    @Provides
    fun provideScoringModeDao(supabase: SupabaseClient): ScoringModeDao = fr.centuryspine.lsgscores.data.scoring.ScoringModeDaoSupabase(supabase)


    @Provides
    fun provideTeamDao(supabase: SupabaseClient): TeamDao = fr.centuryspine.lsgscores.data.session.TeamDaoSupabase(supabase)

    @Provides
    fun providePlayedHoleDao(supabase: SupabaseClient): PlayedHoleDao = fr.centuryspine.lsgscores.data.session.PlayedHoleDaoSupabase(supabase)

    @Provides
    fun providePlayedHoleScoreDao(supabase: SupabaseClient): PlayedHoleScoreDao = fr.centuryspine.lsgscores.data.session.PlayedHoleScoreDaoSupabase(supabase)

    @Provides
    fun provideGameZoneDao(supabase: SupabaseClient): GameZoneDao = fr.centuryspine.lsgscores.data.gamezone.GameZoneDaoSupabase(supabase)

    @Provides
    fun provideCityDao(supabase: SupabaseClient): CityDao = fr.centuryspine.lsgscores.data.city.CityDaoSupabase(supabase)
}