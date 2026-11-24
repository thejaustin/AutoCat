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

package app.lawnchair

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.recents.TaskSwitcherActivity
import app.lawnchair.recents.TaskSwitcherRepository

class AutoCatAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            // Set the type of events that this service wants to listen to.  Others
            // won't be passed to this service.
            // Only window changes, to notice the system recents screen opening
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            // If you only want this service to work with specific applications, set their
            // package names here.  Otherwise, when the service is activated, it will listen
            // to events from all applications.
            packageNames = emptyArray()
        }
        autoCatApp.accessibilityService = this
    }

    override fun onDestroy() {
        autoCatApp.accessibilityService = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onInterrupt() {}

    private val handler = Handler(Looper.getMainLooper())
    private var lastTakeover = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // Quickstep's recents window, in One UI Home, Pixel Launcher or SystemUI alike
        val className = event.className?.toString() ?: return
        if (!className.endsWith(RECENTS_CLASS_SUFFIX)) return
        if (event.packageName == packageName) return
        if (!PreferenceManager.getInstance(this).taskSwitcherTakeover.get()) return
        val now = SystemClock.uptimeMillis()
        if (now - lastTakeover < COOLDOWN_MS) return
        // Never leave the user without a recents screen: only take over when ours can work
        if (TaskSwitcherRepository(this).availability() != TaskSwitcherRepository.Availability.Ready) return
        lastTakeover = now
        Log.d(TAG, "Taking over recents from ${event.packageName}/$className")
        // Close the system recents first so dismissing ours returns to the app underneath
        performGlobalAction(GLOBAL_ACTION_BACK)
        handler.postDelayed({
            startActivity(
                Intent(this, TaskSwitcherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }, OPEN_DELAY_MS)
    }

    private companion object {
        const val TAG = "AutoCatA11y"
        const val RECENTS_CLASS_SUFFIX = "RecentsActivity"
        const val COOLDOWN_MS = 1500L
        const val OPEN_DELAY_MS = 120L
    }
}
