package app.lawnchair.gestures.handlers

import android.content.Context
import app.lawnchair.AutoCatLauncher

class OpenAppSearchGestureHandler(context: Context) : OpenAppDrawerGestureHandler(context) {

    override suspend fun onTrigger(launcher: AutoCatLauncher) {
        super.onTrigger(launcher)
        launcher.appsView.searchUiManager.editText?.showKeyboard()
    }
}
