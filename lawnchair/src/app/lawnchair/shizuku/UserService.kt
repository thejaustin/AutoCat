package app.lawnchair.shizuku

import android.content.Context
import android.util.Log
import kotlin.system.exitProcess

class UserService(context: Context) : IUserService.Stub() {

    override fun runCommand(command: Array<String>, env: Array<String>?) {
        try {
            val process = Runtime.getRuntime().exec(command, env)
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed: ${command.joinToString(" ")}", e)
        }
    }

    companion object {
        private const val TAG = "UserService"
    }

    override fun destroy() {
        exitProcess(0)
    }
}
