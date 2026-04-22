package app.lawnchair.compatlib.ten;

import static android.app.ActivityManager.RECENT_IGNORE_UNAVAILABLE;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.WindowConfiguration;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.RemoteException;
import android.util.Log;
import android.view.IRecentsAnimationController;
import android.view.IRecentsAnimationRunner;
import android.view.RemoteAnimationTarget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import app.lawnchair.compatlib.ActivityManagerCompat;
import app.lawnchair.compatlib.RecentsAnimationRunnerCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(29)
public class ActivityManagerCompatVQ implements ActivityManagerCompat {
    protected final String TAG = getClass().getCanonicalName();

    @Override
    public void invalidateHomeTaskSnapshot(Activity homeActivity) {
        // Do nothing, Android Q doesn't support this.
    }

    @Override
    public List<ActivityManager.RunningTaskInfo> getRunningTasks(boolean filterOnlyVisibleRecents) {
        int ignoreActivityType = WindowConfiguration.ACTIVITY_TYPE_UNDEFINED;
        if (filterOnlyVisibleRecents) {
            ignoreActivityType = WindowConfiguration.ACTIVITY_TYPE_RECENTS;
        }

        try {
            return ActivityTaskManager.getService()
                    .getFilteredTasks(
                            NUM_RECENT_ACTIVITIES_REQUEST,
                            ignoreActivityType,
                            WindowConfiguration.WINDOWING_MODE_PINNED);
        } catch (RemoteException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void startRecentsActivity(
            Intent intent, long eventTime, RecentsAnimationRunnerCompat runnerCompat) {

        IRecentsAnimationRunner runner = null;
        if (runnerCompat != null) {
            runner =
                    new IRecentsAnimationRunner.Stub() {
                        @Override
                        public void onAnimationStart(
                                IRecentsAnimationController controller,
                                RemoteAnimationTarget[] apps,
                                Rect homeContentInsets,
                                Rect minimizedHomeBounds) {
                            runnerCompat.onAnimationStart(
                                    controller, apps, null, homeContentInsets, minimizedHomeBounds);
                        }

                        @Override
                        public void onAnimationCanceled(boolean deferredWithScreenshot) {
                            runnerCompat.onAnimationCanceled(deferredWithScreenshot);
                        }

                        // Samsung OneUi
                        public void reportAllDrawn() {}
                    };
        }
        try {
            ActivityTaskManager.getService().startRecentsActivity(intent, null, runner);
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public ActivityManager.RunningTaskInfo getRunningTask(boolean filterOnlyVisibleRecents) {
        int ignoreActivityType = WindowConfiguration.ACTIVITY_TYPE_UNDEFINED;
        if (filterOnlyVisibleRecents) {
            ignoreActivityType = WindowConfiguration.ACTIVITY_TYPE_RECENTS;
        }
        try {
            List<ActivityManager.RunningTaskInfo> tasks =
                    ActivityTaskManager.getService()
                            .getFilteredTasks(
                                    1,
                                    ignoreActivityType,
                                    WindowConfiguration.WINDOWING_MODE_PINNED);
            if (tasks.isEmpty()) {
                return null;
            }
            return tasks.get(0);
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public List<ActivityManager.RecentTaskInfo> getRecentTasks(int numTasks, int userId) {
        try {
            return ActivityTaskManager.getService()
                    .getRecentTasks(numTasks, RECENT_IGNORE_UNAVAILABLE, userId)
                    .getList();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get recent tasks", e);
            return new ArrayList<>();
        }
    }

    @Override
    public ThumbnailData getTaskThumbnail(int taskId, boolean isLowResolution) {
        Object snapshot = getTaskSnapshotViaReflection(taskId, isLowResolution);
        return snapshot != null ? makeThumbnailData(snapshot) : null;
    }

    //    @Override
    // pE-TODO(QuickSwitch): Investigate
    public ThumbnailData takeScreenshot(
            IRecentsAnimationController animationController, int taskId) {
        try {
            Object snapshot = screenshotTaskViaReflection(animationController, taskId);
            return snapshot != null ? makeThumbnailData(snapshot) : new ThumbnailData();
        } catch (Exception e) {
            Log.e(TAG, "Failed to screenshot task", e);
            return new ThumbnailData();
        }
    }

    @Override
    public ThumbnailData convertTaskSnapshotToThumbnailData(Object taskSnapshot) {
        return taskSnapshot != null ? makeThumbnailData(taskSnapshot) : null;
    }

    // Accepts Object so this compiles without the hidden ActivityManager.TaskSnapshot type.
    @SuppressWarnings("unchecked")
    public ThumbnailData makeThumbnailData(Object snapshot) {
        ThumbnailData data = new ThumbnailData();
        try {
            HardwareBuffer buffer = (HardwareBuffer) invoke(snapshot, "getSnapshot");
            Object colorSpace = invoke(snapshot, "getColorSpace");
            data.thumbnail = Bitmap.wrapHardwareBuffer(
                    buffer, colorSpace instanceof android.graphics.ColorSpace
                            ? (android.graphics.ColorSpace) colorSpace : null);
            Rect contentInsets = (Rect) invoke(snapshot, "getContentInsets");
            data.insets = contentInsets != null ? new Rect(contentInsets) : new Rect();
            data.orientation = (int) invoke(snapshot, "getOrientation");
            data.reducedResolution = (boolean) invoke(snapshot, "isReducedResolution");
            data.scale = (float) invoke(snapshot, "getScale");
            data.isRealSnapshot = (boolean) invoke(snapshot, "isRealSnapshot");
            data.isTranslucent = (boolean) invoke(snapshot, "isTranslucent");
            data.windowingMode = (int) invoke(snapshot, "getWindowingMode");
            data.systemUiVisibility = (int) invoke(snapshot, "getSystemUiVisibility");
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract thumbnail data from snapshot", e);
        }
        return data;
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        Method m = target.getClass().getMethod(methodName);
        return m.invoke(target);
    }

    private Object getTaskSnapshotViaReflection(int taskId, boolean isLowResolution) {
        try {
            Object service = ActivityTaskManager.getService();
            Method m = service.getClass().getMethod("getTaskSnapshot", int.class, boolean.class);
            return m.invoke(service, taskId, isLowResolution);
        } catch (Exception e) {
            Log.w(TAG, "getTaskSnapshot via reflection failed", e);
            return null;
        }
    }

    private static Object screenshotTaskViaReflection(
            IRecentsAnimationController controller, int taskId) {
        try {
            Method m = controller.getClass().getMethod("screenshotTask", int.class);
            return m.invoke(controller, taskId);
        } catch (Exception e) {
            return null;
        }
    }
}
