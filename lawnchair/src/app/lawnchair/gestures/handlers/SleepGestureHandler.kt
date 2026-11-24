/*
 * Copyright 2021, AutoCat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.gestures.handlers

import android.accessibilityservice.AccessibilityService
import android.annotation.TargetApi
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import app.lawnchair.AutoCatLauncher
import app.lawnchair.backend.Backend
import app.lawnchair.backend.BackendSelector
import app.lawnchair.backend.Capability
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.shizuku.ShizukuManager
import app.lawnchair.util.requireSystemService
import app.lawnchair.views.ComposeBottomSheet
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SleepGestureHandler(context: Context) : GestureHandler(context) {

    private val selector = BackendSelector.getInstance(context)

    override suspend fun onTrigger(launcher: AutoCatLauncher) {
        val pref = PreferenceManager2.getInstance(context).sleepMode.get().first()
        if (pref == SleepMode.DEVICE_ADMIN) {
            SleepMethodDeviceAdmin(context).sleep(launcher)
            return
        }

        // The global backend mode and per-backend toggles decide what is usable; the sleep mode
        // can only prefer one of those, never override a disabled or unavailable backend.
        val usable = selector.resolve(Capability.ScreenLock).backends
        val preferred = when (pref) {
            SleepMode.ROOT -> Backend.Root
            SleepMode.ACCESSIBILITY -> Backend.Accessibility
            else -> null
        }
        val order = if (preferred != null && preferred in usable) listOf(preferred) else usable

        for (backend in order) {
            if (lock(backend, launcher)) return
        }

        // Nothing usable: ask the user to enable one of the unprivileged options
        val adminActive = context.requireSystemService<DevicePolicyManager>()
            .isAdminActive(ComponentName(context, SleepMethodDeviceAdmin.SleepDeviceAdmin::class.java))
        if (!adminActive && selector.isEnabled(Backend.Accessibility) && Utilities.ATLEAST_P) {
            lockWithAccessibility(launcher)
        } else {
            SleepMethodDeviceAdmin(context).sleep(launcher)
        }
    }

    private suspend fun lock(backend: Backend, launcher: AutoCatLauncher): Boolean = when (backend) {
        Backend.Root -> withContext(Dispatchers.IO) { Shell.cmd("input keyevent 26").exec().isSuccess }

        Backend.ShizukuPlus, Backend.Shizuku -> withContext(Dispatchers.IO) {
            ShizukuManager.getInstance(context).awaitService()
                ?.runCommand(arrayOf("input", "keyevent", "26"), null) == 0
        }

        Backend.Accessibility -> Utilities.ATLEAST_P && lockWithAccessibility(launcher)

        // Dhizuku is detection-only until a consumer client exists
        Backend.Dhizuku -> false
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun lockWithAccessibility(launcher: AutoCatLauncher): Boolean {
        GestureWithAccessibilityHandler.onTrigger(
            launcher,
            R.string.sleep_a11y_hint,
            AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN,
        )
        return true
    }
}

class SleepMethodDeviceAdmin(private val context: Context) {
    suspend fun sleep(launcher: AutoCatLauncher) {
        val devicePolicyManager: DevicePolicyManager = context.requireSystemService()
        if (!devicePolicyManager.isAdminActive(ComponentName(context, SleepDeviceAdmin::class.java))) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    ComponentName(context, SleepDeviceAdmin::class.java),
                )
                .putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    launcher.getString(R.string.dt2s_admin_hint),
                )
            ComposeBottomSheet.show(launcher) {
                ServiceWarningDialog(
                    title = R.string.dt2s_admin_hint_title,
                    action = R.string.dt2s_admin_hint,
                    settingsIntent = intent,
                ) { close(true) }
            }
            return
        }
        devicePolicyManager.lockNow()
    }

    class SleepDeviceAdmin : DeviceAdminReceiver() {

        override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
            return context.getString(R.string.dt2s_admin_warning)
        }
    }
}
