package app.lawnchair.compatlib.ten;

import android.app.ActivityOptions;
import android.content.Context;
import android.os.Handler;
import android.view.RemoteAnimationAdapter;


import androidx.annotation.RequiresApi;
import app.lawnchair.compatlib.ActivityOptionsCompat;

@RequiresApi(29)
public class ActivityOptionsCompatVQ implements ActivityOptionsCompat {
    protected final String TAG = getClass().getCanonicalName();

    androidx.annotation.NonNull
    @Override
    public ActivityOptions makeCustomAnimation(
            androidx.annotation.NonNull Context context,
            int enterResId,
            int exitResId,
            androidx.annotation.NonNull final Handler callbackHandler,
            androidx.annotation.Nullable final Runnable startedListener,
            androidx.annotation.Nullable final Runnable finishedListener) {
        return ActivityOptions.makeCustomAnimation(
                context,
                enterResId,
                exitResId,
                callbackHandler,
                new ActivityOptions.OnAnimationStartedListener() {
                    @Override
                    public void onAnimationStarted() {
                        if (startedListener != null) {
                            startedListener.run();
                        }
                    }
                });
    }

    androidx.annotation.NonNull
    @Override
    public ActivityOptions makeRemoteAnimation(
            androidx.annotation.Nullable RemoteAnimationAdapter remoteAnimationAdapter,
            androidx.annotation.Nullable Object remoteTransition,
            androidx.annotation.Nullable String debugName) {
        return ActivityOptions.makeRemoteAnimation(remoteAnimationAdapter);
    }
}
