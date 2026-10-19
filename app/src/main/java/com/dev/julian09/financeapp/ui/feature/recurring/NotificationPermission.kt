package com.dev.julian09.financeapp.ui.feature.recurring

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Asks for POST_NOTIFICATIONS when it is missing (Android 13+; earlier versions need none), then runs [onDone]
 * whatever the answer: a denial only means no notices, the app works the same.
 */
@Composable
fun rememberNotificationPermissionRequest(onDone: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    return {
        val missing =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (missing) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onDone()
    }
}
