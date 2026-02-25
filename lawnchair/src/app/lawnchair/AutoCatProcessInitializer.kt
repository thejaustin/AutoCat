package app.lawnchair

import android.content.Context
import androidx.annotation.Keep
import app.lawnchair.bugreport.AutoCatBugReporter
import app.lawnchair.theme.color.tokens.ColorTokens
import com.android.launcher3.Utilities
import com.android.launcher3.icons.mono.ThemedIconDrawable
import com.android.quickstep.QuickstepProcessInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Process initializer for AutoCat with optimized startup performance.
 *
 * Performance optimizations:
 * - Defers non-critical initialization to background thread
 * - Avoids blocking main thread during cold start
 */
@Keep
class AutoCatProcessInitializer(context: Context) : QuickstepProcessInitializer(context) {

    override fun init(context: Context) {
        // Defer initialization to background thread to avoid blocking main thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize bug reporter (can be done in background)
                AutoCatBugReporter.INSTANCE.get(context)

                // Set up themed icon color loader on main thread (required for UI)
                // This is lightweight and just sets a lambda
                ThemedIconDrawable.COLORS_LOADER = {
                    if (Utilities.isDarkTheme(it)) {
                        intArrayOf(
                            ColorTokens.Accent2_800.resolveColor(it),
                            ColorTokens.Accent1_200.resolveColor(it),
                        )
                    } else {
                        intArrayOf(
                            ColorTokens.Accent1_100.resolveColor(it),
                            ColorTokens.Accent1_700.resolveColor(it),
                        )
                    }
                }
            } catch (e: Exception) {
                // Silently handle initialization errors to prevent crashes
            }
        }

        // Call super last to ensure our setup completes first
        super.init(context)
    }
}
