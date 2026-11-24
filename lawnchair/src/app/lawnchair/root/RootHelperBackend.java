package app.lawnchair.root;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.core.content.ContextCompat;

import app.lawnchair.backend.PackageOps;
import app.lawnchair.backend.PmResult;
import app.lawnchair.backend.RootPmExecutor;

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
        return format(PackageOps.INSTANCE.archive(RootPmExecutor.INSTANCE, packageName));
    }

    @Override
    public String uninstallPackageKeepData(String packageName) {
        return format(PackageOps.INSTANCE.uninstall(RootPmExecutor.INSTANCE, packageName, true));
    }

    private static String format(PmResult result) {
        if (result.getSuccess()) {
            return "success";
        }
        return "error: " + (result.getError().isEmpty() ? "unknown" : result.getError());
    }
}
