// MainActivity.kt

package com.example.lsgscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import androidx.navigation.compose.rememberNavController
import com.example.lsgscores.data.AppDatabase
import com.example.lsgscores.data.hole.HoleRepository
import com.example.lsgscores.data.media.MediaRepository
import com.example.lsgscores.data.scoring.ScoringModeRepository
import com.example.lsgscores.data.session.SessionRepository
import com.example.lsgscores.data.session.TeamRepository
import com.example.lsgscores.data.user.UserRepository
import com.example.lsgscores.ui.MainScreen
import com.example.lsgscores.viewmodel.HoleViewModel
import com.example.lsgscores.viewmodel.SessionViewModel
import com.example.lsgscores.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "streetgolf-db"
        ).fallbackToDestructiveMigration().build()

        val userRepository = UserRepository(db.userDao())
        val holeRepository = HoleRepository(db.holeDao())
        val sessionRepository = SessionRepository(db.sessionDao())
        val teamRepository = TeamRepository(db.teamDao())
        val mediaRepository = MediaRepository(db.mediaDao())
        val scoringModeRepository = ScoringModeRepository() // hardcoded

        setContent {
            val navController = rememberNavController()

            val userViewModel = UserViewModel(userRepository)
            val holeViewModel = HoleViewModel(holeRepository)
            val sessionViewModel = SessionViewModel(
                sessionRepository = sessionRepository,
                teamRepository = teamRepository,
                mediaRepository = mediaRepository,
                scoringModeRepository = scoringModeRepository
            )

            MainScreen(
                navController = navController,
                userViewModel = userViewModel,
                holeViewModel = holeViewModel,
                sessionViewModel = sessionViewModel
            )
        }
    }
}
