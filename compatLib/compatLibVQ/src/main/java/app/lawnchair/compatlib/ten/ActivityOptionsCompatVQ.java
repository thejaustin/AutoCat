package app.lawnchair.compatlib.ten;

import android.app.ActivityOptions;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.RemoteAnimationAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import app.lawnchair.compatlib.ActivityOptionsCompat;

import java.lang.reflect.Method;

@RequiresApi(29)
public class ActivityOptionsCompatVQ implements ActivityOptionsCompat {
    protected final String TAG = getClass().getCanonicalName();

    @Override
    public ActivityOptions makeCustomAnimation(
            @NonNull Context context,
            int enterResId,
            int exitResId,
            @NonNull final Handler callbackHandler,
            @Nullable final Runnable startedListener,
            @Nullable final Runnable finishedListener) {
        // OnAnimationStartedListener is a @hide interface — use the 3-arg public overload.
        // startedListener/finishedListener are best-effort; skipped on API 29 since
        // the hidden 5-arg overload is not available to this compiler target.
        return ActivityOptions.makeCustomAnimation(context, enterResId, exitResId);
    }

    @Override
    public ActivityOptions makeRemoteAnimation(
            @Nullable RemoteAnimationAdapter remoteAnimationAdapter,
            @Nullable Object remoteTransition,
            @Nullable String debugName) {
        // ActivityOptions.makeRemoteAnimation(RemoteAnimationAdapter) is @hide.
        // Invoke via reflection; fall back to makeBasic() when unavailable.
        if (remoteAnimationAdapter != null) {
            try {
                Method m = ActivityOptions.class.getMethod(
                        "makeRemoteAnimation", RemoteAnimationAdapter.class);
                Object result = m.invoke(null, remoteAnimationAdapter);
                if (result instanceof ActivityOptions) return (ActivityOptions) result;
            } catch (Exception e) {
                Log.w(TAG, "makeRemoteAnimation via reflection failed", e);
            }
        }
        return ActivityOptions.makeBasic();
    }
}
