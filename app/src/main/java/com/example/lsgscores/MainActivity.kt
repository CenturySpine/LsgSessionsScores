package com.example.lsgscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.lsgscores.data.AppDatabase
import com.example.lsgscores.data.UserRepository



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "streetgolf-db"
        ).build()
        val userRepository = UserRepository(db.userDao())

        setContent {
            AppNavHost(application)
        }
    }
}
