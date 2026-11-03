package com.dev.julian09.financeapp

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dev.julian09.financeapp.ui.navigation.PendingLink
import com.dev.julian09.financeapp.ui.root.AppRoot
import com.dev.julian09.financeapp.ui.root.FirstContent
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
        keepSplashUntilContent()
    }

    /** #223: the system splash (Android 12+) stays instead of a blank screen while the session and Inicio load. */
    private fun keepSplashUntilContent() {
        val content = findViewById<View>(android.R.id.content)
        val start = SystemClock.uptimeMillis()
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // the cap: a load that never ends must not freeze the app on the splash
                    val ready = FirstContent.shown || SystemClock.uptimeMillis() - start > MAX_SPLASH_MS
                    if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                    return ready
                }
            },
        )
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
        intent?.getStringExtra(EXTRA_CARD_ID)?.let { id ->
            runCatching { UUID.fromString(id) }.onSuccess { PendingLink.cardId.value = it }
        }
        intent?.getStringExtra(EXTRA_LOAN_ID)?.let { id ->
            runCatching { UUID.fromString(id) }.onSuccess { PendingLink.loanId.value = it }
        }
    }

    companion object {
        const val EXTRA_RECURRING_ID = "recurringId"
        const val EXTRA_CARD_ID = "cardId"
        const val EXTRA_LOAN_ID = "loanId"
        private const val MAX_SPLASH_MS = 4_000L
    }
}
