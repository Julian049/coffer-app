package com.dev.julian09.financeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dev.julian09.financeapp.ui.navigation.PendingLink
import com.dev.julian09.financeapp.ui.root.AppRoot
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Not on a recreation: the notice was already handled
        if (savedInstanceState == null) openPending(intent)
        enableEdgeToEdge()
        setContent {
            CofferTheme {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openPending(intent)
    }

    private fun openPending(intent: Intent?) {
        intent?.getStringExtra(EXTRA_RECURRING_ID)?.let { id ->
            runCatching { UUID.fromString(id) }.onSuccess {
                PendingLink.recurringId.value =
                    it
            }
        }
    }

    companion object {
        const val EXTRA_RECURRING_ID = "recurringId"
    }
}
