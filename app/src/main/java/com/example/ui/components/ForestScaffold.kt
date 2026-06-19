package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ForestScaffold(
    forestViewModel: ForestViewModel = viewModel(),
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val forestState by forestViewModel.forestState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: forest — always behind everything
        ForestBackground(
            forestState = forestState,
            modifier    = Modifier.fillMaxSize()
        )
        // Layer 2: app content with transparent scaffold
        Scaffold(
            containerColor = Color.Transparent,
            contentColor   = if (forestState.isDayTime) Color(0xFFE0F0E8) else Color.White,
            bottomBar      = bottomBar
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}
