//package com.example.lsgscores
//
//import android.app.Application
//import androidx.compose.runtime.Composable
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.example.lsgscores.data.UserRepository
//import com.example.lsgscores.ui.users.*
//
//@Composable
//fun AppNavHost(repository: UserRepository) {
//    val navController = rememberNavController()
//    val userViewModel: UserViewModel = viewModel(
//        factory = UserViewModelFactory(repository)
//    )
//
//    NavHost(navController, startDestination = "user_list") {
//        composable("user_list") {
//            UserListScreen(
//                navController = navController,
//                userViewModel = userViewModel
//            )
//        }
//        composable("add_user") {
//            UserFormScreen(
//                navController = navController,
//                userViewModel = userViewModel
//            )
//        }
//    }
//}
