package app.lawnchair.gestures.handlers

import android.content.Context
import app.lawnchair.AutoCatLauncher
import app.lawnchair.animateToAllApps

open class OpenAppDrawerGestureHandler(context: Context) : GestureHandler(context) {

    override suspend fun onTrigger(launcher: AutoCatLauncher) {
        launcher.animateToAllApps()
    }
}
