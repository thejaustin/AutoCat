package app.lawnchair.compatlib.fifteen;

import android.app.ActivityOptions;
import android.content.Context;
import android.os.Handler;


import androidx.annotation.RequiresApi;
import app.lawnchair.compatlib.fourteen.ActivityOptionsCompatVU;

@RequiresApi(35)
public class ActivityOptionsCompatVV extends ActivityOptionsCompatVU {
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
                0,
                callbackHandler,
                new ActivityOptions.OnAnimationStartedListener() {
                    @Override
                    public void onAnimationStarted(long elapsedRealTime) {
                        if (startedListener != null) {
                            startedListener.run();
                        }
                    }
                },
                new ActivityOptions.OnAnimationFinishedListener() {
                    @Override
                    public void onAnimationFinished(long elapsedRealTime) {
                        if (finishedListener != null) {
                            finishedListener.run();
                        }
                    }
                });
    }
}
