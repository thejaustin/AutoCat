package app.lawnchair.backend

import android.os.Build
import android.util.Log
import app.lawnchair.shizuku.IUserService
import com.topjohnwu.superuser.Shell
import java.io.File

data class PmResult(val success: Boolean, val error: String = "")

/** Runs a `pm` argument list through one privileged transport. Blocking: call off the main thread. */
fun interface PmExecutor {
    fun exec(args: List<String>): PmResult
}

/** Executes as root through libsu. Arguments are validated before being joined into a shell line. */
object RootPmExecutor : PmExecutor {
    override fun exec(args: List<String>): PmResult {
        val result = Shell.cmd(args.joinToString(" ") { quote(it) }).exec()
        return PmResult(result.isSuccess, result.err.joinToString("\n"))
    }

    private fun quote(arg: String) = if (arg.all { it.isLetterOrDigit() || it in "_.-/" }) arg else "'${arg.replace("'", "'\\''")}'"
}

/** Executes as the shell user through an [IUserService] (Shizuku-hosted). No shell parsing involved. */
class ServicePmExecutor(private val service: IUserService) : PmExecutor {
    override fun exec(args: List<String>): PmResult = try {
        val code = service.runCommand(args.toTypedArray(), null)
        PmResult(code == 0, if (code == 0) "" else "exit code $code")
    } catch (e: Exception) {
        Log.e(TAG, "Command failed: ${args.joinToString(" ")}", e)
        PmResult(false, e.message ?: "command failed")
    }

    private companion object {
        const val TAG = "ServicePmExecutor"
    }
}

/** The single implementation of AutoCat's package-state commands, whichever backend runs them. */
object PackageOps {
    private const val TAG = "PackageOps"
    private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z0-9_.]+$")

    private fun validated(packageName: String): String? = packageName.takeIf { it.matches(PACKAGE_NAME_REGEX) } ?: run {
        Log.e(TAG, "Invalid package name rejected: $packageName")
        null
    }

    /** `pm archive` on Android 15+, falling back to `pm uninstall -k` (keeps data) where unsupported. */
    fun archive(executor: PmExecutor, packageName: String): PmResult {
        val pkg = validated(packageName) ?: return PmResult(false, "invalid package name")
        if (Build.VERSION.SDK_INT >= 35) {
            val result = executor.exec(listOf("pm", "archive", pkg))
            if (result.success) return result
            Log.w(TAG, "pm archive failed, falling back to uninstall -k: ${result.error}")
        }
        return uninstall(executor, pkg, keepData = true)
    }

    fun uninstall(executor: PmExecutor, packageName: String, keepData: Boolean): PmResult {
        val pkg = validated(packageName) ?: return PmResult(false, "invalid package name")
        return executor.exec(if (keepData) listOf("pm", "uninstall", "-k", pkg) else listOf("pm", "uninstall", pkg))
    }

    fun disable(executor: PmExecutor, packageName: String): PmResult {
        val pkg = validated(packageName) ?: return PmResult(false, "invalid package name")
        return executor.exec(listOf("pm", "disable-user", "--user", "0", pkg))
    }

    fun enable(executor: PmExecutor, packageName: String): PmResult {
        val pkg = validated(packageName) ?: return PmResult(false, "invalid package name")
        return executor.exec(listOf("pm", "enable", pkg))
    }

    fun install(executor: PmExecutor, file: File): PmResult = executor.exec(listOf("pm", "install", "-r", file.absolutePath))
}
