package app.lawnchair.shizuku

import android.content.Context
import android.os.IBinder
import android.os.Process
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class UserService(context: Context) : IUserService.Stub() {

    override fun runCommand(command: Array<String>, env: Array<String>?) {
        try {
            val process = Runtime.getRuntime().exec(command, env)
            process.waitFor()
            // Optionally we could read output here if needed, but for now we just wait.
            // If output is needed, we would need to pass a callback or return values.
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun destroy() {
        exitProcess(0)
    }
}
