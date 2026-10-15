package com.zeroos.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.zeroos.core.resources.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A launcher has nowhere to go back to: Back on home does nothing (Step 1.1).
        // Screens opened on top of home (app list, diagnostics) handle Back themselves.
        onBackPressedDispatcher.addCallback(this) {}
        // Placeholder until the design system (Step 2.2) and home screen (Step 1.3) exist.
        setContent {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.app_name), color = Color.White)
            }
        }
    }
}
