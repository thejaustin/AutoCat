package app.lawnchair.compatlib.eleven;

import android.app.ActivityOptions;
import android.content.Context;
import android.os.Handler;


import androidx.annotation.RequiresApi;
import app.lawnchair.compatlib.ten.ActivityOptionsCompatVQ;

@RequiresApi(30)
public class ActivityOptionsCompatVR extends ActivityOptionsCompatVQ {

    @Override
    public ActivityOptions makeCustomAnimation(
            @androidx.annotation.NonNull Context context,
            int enterResId,
            int exitResId,
            @androidx.annotation.NonNull final Handler callbackHandler,
            @androidx.annotation.Nullable final Runnable startedListener,
            @androidx.annotation.Nullable final Runnable finishedListener) {
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
                },
                new ActivityOptions.OnAnimationFinishedListener() {
                    @Override
                    public void onAnimationFinished() {
                        if (finishedListener != null) {
                            finishedListener.run();
                        }
                    }
                });
    }
}
