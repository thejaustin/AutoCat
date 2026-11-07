package app.lawnchair.gestures.handlers

import android.content.Context
import android.content.Intent
import app.lawnchair.AutoCatLauncher
import app.lawnchair.recents.TaskSwitcherActivity

class OpenTaskSwitcherHandler(context: Context) : GestureHandler(context) {

    override suspend fun onTrigger(launcher: AutoCatLauncher) {
        launcher.startActivity(Intent(launcher, TaskSwitcherActivity::class.java))
    }
}
