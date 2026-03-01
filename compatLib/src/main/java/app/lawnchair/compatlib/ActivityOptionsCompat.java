package app.lawnchair.compatlib;

import android.app.ActivityOptions;
import android.content.Context;
import android.os.Handler;
import android.view.RemoteAnimationAdapter;



public interface ActivityOptionsCompat {

    androidx.annotation.NonNull
    ActivityOptions makeCustomAnimation(
            androidx.annotation.NonNull Context context,
            int enterResId,
            int exitResId,
            androidx.annotation.NonNull final Handler callbackHandler,
            androidx.annotation.Nullable final Runnable startedListener,
            androidx.annotation.Nullable final Runnable finishedListener);

    androidx.annotation.NonNull
    ActivityOptions makeRemoteAnimation(
            androidx.annotation.Nullable RemoteAnimationAdapter remoteAnimationAdapter,
            androidx.annotation.Nullable Object remoteTransition,
            androidx.annotation.Nullable String debugName);
}
