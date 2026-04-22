package app.lawnchair.compat

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import app.lawnchair.compatlib.ActivityManagerCompat
import app.lawnchair.compatlib.ActivityOptionsCompat
import app.lawnchair.compatlib.QuickstepCompatFactory
import app.lawnchair.compatlib.RemoteTransitionCompat

/**
 * Java-visible compatibility shim. Delegates to [AutoCatQuickstepCompat].
 * Kept because upstream systemUI/quickstep Java sources still reference this name.
 */
object LawnchairQuickstepCompat {

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    @JvmField
    val ATLEAST_Q: Boolean = AutoCatQuickstepCompat.ATLEAST_Q

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    @JvmField
    val ATLEAST_R: Boolean = AutoCatQuickstepCompat.ATLEAST_R

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    @JvmField
    val ATLEAST_S: Boolean = AutoCatQuickstepCompat.ATLEAST_S

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    @JvmField
    val ATLEAST_T: Boolean = AutoCatQuickstepCompat.ATLEAST_T

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @JvmField
    val ATLEAST_U: Boolean = AutoCatQuickstepCompat.ATLEAST_U

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @JvmField
    val ATLEAST_V: Boolean = AutoCatQuickstepCompat.ATLEAST_V

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BAKLAVA)
    @JvmField
    val ATLEAST_BAKLAVA: Boolean = AutoCatQuickstepCompat.ATLEAST_BAKLAVA

    @JvmStatic
    val factory: QuickstepCompatFactory get() = AutoCatQuickstepCompat.factory

    @JvmStatic
    val activityManagerCompat: ActivityManagerCompat get() = AutoCatQuickstepCompat.activityManagerCompat

    @JvmStatic
    val activityOptionsCompat: ActivityOptionsCompat get() = AutoCatQuickstepCompat.activityOptionsCompat

    @JvmStatic
    val remoteTransitionCompat: RemoteTransitionCompat get() = AutoCatQuickstepCompat.remoteTransitionCompat
}
