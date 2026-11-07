/*
 * Copyright 2026 MinGyu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gyugle.gil.core.presentation.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Asks for and checks runtime permissions from a screen's Root composable.
 *
 * Get it from [rememberPermissionController]; it holds the current activity and must not outlive it.
 */
class PermissionController internal constructor(private val activity: ComponentActivity) {

    // The system shows one permission dialog at a time; overlapping requests lose a result.
    private val requestMutex = Mutex()

    /**
     * Shows the system dialog when needed and returns the result. Two answers can be wrong: closing
     * the first dialog with back comes back as [PermissionState.PERMANENTLY_DENIED] although the
     * dialog would show again, and a rotation while the dialog is open drops the result. Check
     * [getPermissionState] again when the screen resumes.
     */
    suspend fun requestPermission(permission: Permission): PermissionState {
        val manifestPermission = permission.toManifestPermission() ?: return PermissionState.GRANTED
        if (isGranted(manifestPermission)) return PermissionState.GRANTED
        return requestMutex.withLock {
            if (isGranted(manifestPermission)) return@withLock PermissionState.GRANTED
            val granted = requestFromSystem(manifestPermission)
            resolveStateAfterRequest(granted, activity.shouldShowRequestPermissionRationale(manifestPermission))
        }
    }

    fun getPermissionState(permission: Permission): PermissionState {
        val manifestPermission = permission.toManifestPermission() ?: return PermissionState.GRANTED
        return resolveQueriedState(
            granted = isGranted(manifestPermission),
            showRationale = activity.shouldShowRequestPermissionRationale(manifestPermission)
        )
    }

    /** Opens this app's page in system settings. Check the state again when the screen resumes. */
    fun openAppSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null))
        )
    }

    private fun isGranted(manifestPermission: String): Boolean =
        ContextCompat.checkSelfPermission(activity, manifestPermission) == PackageManager.PERMISSION_GRANTED

    /**
     * Registers without a LifecycleOwner, which is allowed after the activity has started, so the
     * launcher must be unregistered on both the result and the cancellation path.
     */
    private suspend fun requestFromSystem(manifestPermission: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val key = "permission:$manifestPermission:${SystemClock.elapsedRealtimeNanos()}"
            lateinit var launcher: ActivityResultLauncher<String>
            launcher =
                activity.activityResultRegistry.register(key, ActivityResultContracts.RequestPermission()) { granted ->
                    launcher.unregister()
                    if (continuation.isActive) continuation.resume(granted)
                }
            continuation.invokeOnCancellation { launcher.unregister() }
            launcher.launch(manifestPermission)
        }
}
