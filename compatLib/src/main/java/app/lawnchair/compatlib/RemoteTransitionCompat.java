package app.lawnchair.compatlib;

import android.app.IApplicationThread;
import android.window.IRemoteTransition;
import android.window.RemoteTransition;



public interface RemoteTransitionCompat {

    RemoteTransition getRemoteTransition(
            androidx.annotation.NonNull IRemoteTransition remoteTransition,
            androidx.annotation.Nullable IApplicationThread appThread,
            androidx.annotation.Nullable String debugName);
}
