package fr.centuryspine.lsgscores.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import fr.centuryspine.lsgscores.data.hole.Hole
import fr.centuryspine.lsgscores.data.hole.HoleDao
import fr.centuryspine.lsgscores.data.media.Media
import fr.centuryspine.lsgscores.data.media.MediaDao
import fr.centuryspine.lsgscores.data.player.Player
import fr.centuryspine.lsgscores.data.player.PlayerDao
import fr.centuryspine.lsgscores.data.scoring.ScoringMode
import fr.centuryspine.lsgscores.data.scoring.ScoringModeDao
import fr.centuryspine.lsgscores.data.session.PlayedHole
import fr.centuryspine.lsgscores.data.session.PlayedHoleDao
import fr.centuryspine.lsgscores.data.session.PlayedHoleScore
import fr.centuryspine.lsgscores.data.session.PlayedHoleScoreDao
import fr.centuryspine.lsgscores.data.session.Session
import fr.centuryspine.lsgscores.data.session.SessionDao
import fr.centuryspine.lsgscores.data.session.Team
import fr.centuryspine.lsgscores.data.session.TeamDao

@Database(
    entities = [Player::class, Hole::class, Session::class, ScoringMode::class, Media::class, Team::class, PlayedHole::class, PlayedHoleScore::class, GameZone::class],
    version = 6
)
@TypeConverters(DateTimeConverters::class,WeatherConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): PlayerDao
    abstract fun holeDao(): HoleDao

    abstract fun sessionDao(): SessionDao

    abstract fun scoringModeDao(): ScoringModeDao

    abstract fun mediaDao(): MediaDao

    abstract fun teamDao(): TeamDao

    abstract fun playedHoleDao(): PlayedHoleDao

    abstract fun playedHoleScoreDao(): PlayedHoleScoreDao

    abstract fun gameZoneDao(): GameZoneDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

    }
}
