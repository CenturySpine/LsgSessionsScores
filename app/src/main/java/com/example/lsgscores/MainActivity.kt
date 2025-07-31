package com.example.lsgscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import androidx.navigation.compose.rememberNavController
import com.example.lsgscores.data.AppDatabase
import com.example.lsgscores.data.UserRepository
import com.example.lsgscores.ui.MainScreen
import com.example.lsgscores.ui.users.UserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Création de la base et du repo (OK ici en test sans DI)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "streetgolf-db"
        ).fallbackToDestructiveMigration()
            .build()
        val userRepository = UserRepository(db.userDao())

        setContent {
            val navController = rememberNavController()

            // Création du ViewModel à la main
            val userViewModel = UserViewModel(userRepository)


            MainScreen(navController, userViewModel)
        }
    }
}
