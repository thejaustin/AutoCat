package app.lawnchair.root;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.core.content.ContextCompat;

import com.topjohnwu.superuser.Shell;

import java.util.List;

public class RootHelperBackend extends IRootHelper.Stub {
    private final Context mContext;

    public RootHelperBackend(Context context) {
        mContext = context;
    }

    private PowerManager getPowerManager() {
        return ContextCompat.getSystemService(mContext, PowerManager.class);
    }

    @Override
    public void goToSleep() {
        getPowerManager().goToSleep(SystemClock.uptimeMillis());
    }

    @Override
    public String archivePackage(String packageName) {
        if (Build.VERSION.SDK_INT >= 35) {
            Shell.Result result = Shell.cmd("pm archive " + packageName).exec();
            if (result.isSuccess()) {
                return "success";
            }
            // Fallback to uninstall -k if pm archive not supported on this ROM
            return uninstallPackageKeepData(packageName);
        }
        return uninstallPackageKeepData(packageName);
    }

    @Override
    public String uninstallPackageKeepData(String packageName) {
        Shell.Result result = Shell.cmd("pm uninstall -k " + packageName).exec();
        if (result.isSuccess()) {
            return "success";
        }
        List<String> err = result.getErr();
        return "error: " + (err.isEmpty() ? "unknown" : String.join("\n", err));
    }
}
