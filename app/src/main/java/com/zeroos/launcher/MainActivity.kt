package com.zeroos.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zeroos.core.ui.ZeroTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Emits when Home is pressed while ZeroOS is already in front (Step 1.1). */
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A launcher has nowhere to go back to: Back on home does nothing (Step 1.1).
        // Screens opened on top of home (app list, diagnostics) handle Back themselves.
        onBackPressedDispatcher.addCallback(this) {}
        setContent {
            ZeroTheme {
                LauncherRoot(homePresses)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.tryEmit(Unit)
    }
}
