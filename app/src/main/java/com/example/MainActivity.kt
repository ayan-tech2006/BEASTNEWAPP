package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.MainLayoutContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BeastViewModel
import com.example.ui.viewmodel.BeastViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup premium full bleeds edge-to-edge (including camera notch overlaps)
        enableEdgeToEdge()

        // Core central state container instance
        val viewModel: BeastViewModel by viewModels {
            BeastViewModelFactory(application)
        }

        setContent {
            MyApplicationTheme {
                MainLayoutContainer(viewModel = viewModel)
            }
        }
    }
}
