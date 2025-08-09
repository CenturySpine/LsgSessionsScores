// MainActivity.kt

package com.example.lsgscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import androidx.navigation.compose.rememberNavController
import com.example.lsgscores.data.AppDatabase
import com.example.lsgscores.data.hole.HoleRepository
import com.example.lsgscores.data.holemode.HoleGameModeRepository
import com.example.lsgscores.data.media.MediaRepository
import com.example.lsgscores.data.scoring.ScoringModeRepository
import com.example.lsgscores.data.session.SessionRepository
import com.example.lsgscores.data.session.TeamRepository
import com.example.lsgscores.data.player.PlayerRepository
import com.example.lsgscores.data.session.PlayedHoleRepository
import com.example.lsgscores.data.session.PlayedHoleScoreRepository
import com.example.lsgscores.ui.MainScreen
import com.example.lsgscores.viewmodel.HoleViewModel
import com.example.lsgscores.viewmodel.SessionViewModel
import com.example.lsgscores.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "streetgolf-db"
        ).fallbackToDestructiveMigration().build()

        val playerRepository = PlayerRepository(db.userDao())
        val holeRepository = HoleRepository(db.holeDao())

        val teamRepository = TeamRepository(db.teamDao())
        val mediaRepository = MediaRepository(db.mediaDao())
        val scoringModeRepository = ScoringModeRepository() // hardcoded
        val playedHoleRepository = PlayedHoleRepository(db.playedHoleDao())
        val holeGameModeRepository = HoleGameModeRepository()
        val playedHoleScoreRepository = PlayedHoleScoreRepository(db.playedHoleScoreDao())
        val sessionRepository = SessionRepository(
            db.sessionDao(),
            db.teamDao(),
            db.playedHoleDao(),
            db.playedHoleScoreDao())

        setContent {
            val navController = rememberNavController()

            val playerViewModel = PlayerViewModel(playerRepository)
            val holeViewModel = HoleViewModel(holeRepository)
            val sessionViewModel = SessionViewModel(
                sessionRepository = sessionRepository,
                teamRepository = teamRepository,
                holeRepository = holeRepository,
                mediaRepository = mediaRepository,
                scoringModeRepository = scoringModeRepository,
                playedHoleRepository = playedHoleRepository,
                holeGameModeRepository = holeGameModeRepository,
                playedHoleScoreRepository = playedHoleScoreRepository
            )

            MainScreen(
                navController = navController,
                playerViewModel = playerViewModel,
                holeViewModel = holeViewModel,
                sessionViewModel = sessionViewModel
            )
        }
    }
}
