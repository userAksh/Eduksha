package com.dobi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.dobi.app.core.navigation.DobiNavGraph
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DobiTheme {
                val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                startDestination?.let { dest ->
                    DobiNavGraph(
                        navController = navController,
                        startDestination = dest
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DobiBackground)
                )
            }
        }
    }
}
