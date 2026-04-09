package com.runup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.runup.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "login") {
                    composable("login")         { LoginScreen(navController) }
                    composable("register")      { RegisterScreen(navController) }
                    composable("menu")          { MenuScreen(navController) }
                    composable("profile")       { ProfileScreen(navController) }
                    composable("zapas")         { ZapasScreen(navController) }
                    composable("entrenos")      { EntrenosScreen(navController) }
                    composable("carreras")      { CarrerasScreen(navController) }
                    composable("map")           { MapScreen() }
                    composable("planificacion") { PlanificacionScreen(navController) }
                    composable("alimentacion")  { AlimentacionScreen(navController) }
                    composable("admin") { AdminScreen(navController) }
                }
            }
        }
    }
}