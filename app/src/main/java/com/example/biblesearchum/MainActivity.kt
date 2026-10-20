package com.example.biblesearchum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.biblesearchum.ui.BibleSearchScreen
import com.example.biblesearchum.ui.theme.BibleSearchUMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BibleSearchUMTheme {
                BibleSearchScreen(viewModel = viewModel())
            }
        }
    }
}
