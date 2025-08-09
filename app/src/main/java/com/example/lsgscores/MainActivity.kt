package com.example.lsgscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.lsgscores.ui.MainScreen
import com.example.lsgscores.viewmodel.HoleViewModel
import com.example.lsgscores.viewmodel.PlayerViewModel
import com.example.lsgscores.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            MainScreen(
                navController = navController,
                playerViewModel = hiltViewModel<PlayerViewModel>(),
                holeViewModel = hiltViewModel<HoleViewModel>(),
                sessionViewModel = hiltViewModel<SessionViewModel>()
            )
        }
    }
}