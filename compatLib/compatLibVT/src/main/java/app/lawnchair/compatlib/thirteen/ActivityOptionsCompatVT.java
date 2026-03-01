package app.lawnchair.compatlib.thirteen;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.app.ActivityOptions;
import android.content.Context;
import android.os.Handler;
import android.view.RemoteAnimationAdapter;
import android.window.RemoteTransition;


import androidx.annotation.RequiresApi;
import app.lawnchair.compatlib.twelve.ActivityOptionsCompatVS;

@RequiresApi(33)
public class ActivityOptionsCompatVT extends ActivityOptionsCompatVS {

    @Override
    public ActivityOptions makeRemoteAnimation(
            @Nullable RemoteAnimationAdapter remoteAnimationAdapter,
            @Nullable Object remoteTransition,
            @Nullable String debugName) {
        return ActivityOptions.makeRemoteAnimation(
                remoteAnimationAdapter, (RemoteTransition) remoteTransition);
    }

    @Override
    public ActivityOptions makeCustomAnimation(
            @NonNull Context context,
            int enterResId,
            int exitResId,
            @NonNull Handler callbackHandler,
            Runnable callback,
            Runnable finishedListener) {
        return ActivityOptions.makeCustomTaskAnimation(
                context,
                enterResId,
                exitResId,
                callbackHandler,
                elapsedRealTime -> {
                    if (callback != null) {
                        callbackHandler.post(callback);
                    }
                },
                null /* finishedListener */);
    }
}
