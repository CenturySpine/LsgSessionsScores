package com.example.lsgscores.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lsgscores.data.hole.Hole
import com.example.lsgscores.data.hole.HoleDao
import com.example.lsgscores.data.scoring.ScoringMode
import com.example.lsgscores.data.scoring.ScoringModeDao
import com.example.lsgscores.data.session.Session
import com.example.lsgscores.data.session.Team
import com.example.lsgscores.data.session.SessionDao
import com.example.lsgscores.data.player.Player
import com.example.lsgscores.data.player.PlayerDao
import com.example.lsgscores.data.media.Media
import com.example.lsgscores.data.media.MediaDao
import com.example.lsgscores.data.session.PlayedHole
import com.example.lsgscores.data.session.PlayedHoleScore
import com.example.lsgscores.data.session.PlayedHoleDao
import com.example.lsgscores.data.session.PlayedHoleScoreDao
import com.example.lsgscores.data.session.TeamDao

@Database(
    entities = [Player::class, Hole::class, Session::class, ScoringMode::class, Media::class, Team::class, PlayedHole::class, PlayedHoleScore::class],
    version = 2
)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): PlayerDao
    abstract fun holeDao(): HoleDao

    abstract fun sessionDao(): SessionDao

    abstract fun scoringModeDao(): ScoringModeDao

    abstract fun mediaDao(): MediaDao

    abstract fun teamDao(): TeamDao

    abstract fun playedHoleDao(): PlayedHoleDao

    abstract fun playedHoleScoreDao(): PlayedHoleScoreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

    }
}
