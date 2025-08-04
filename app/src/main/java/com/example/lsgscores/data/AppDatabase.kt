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
import com.example.lsgscores.data.user.User
import com.example.lsgscores.data.user.UserDao
import com.example.lsgscores.data.media.Media
import com.example.lsgscores.data.media.MediaDao
import com.example.lsgscores.data.session.TeamDao

@Database(entities = [User::class, Hole::class, Session::class, ScoringMode::class, Media::class, Team::class], version = 1)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun holeDao(): HoleDao

    abstract fun sessionDao(): SessionDao

    abstract fun scoringModeDao(): ScoringModeDao

    abstract fun mediaDao(): MediaDao

    abstract fun teamDao(): TeamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
