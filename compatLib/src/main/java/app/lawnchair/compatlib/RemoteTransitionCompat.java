package app.lawnchair.compatlib;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.app.IApplicationThread;
import android.window.IRemoteTransition;
import android.window.RemoteTransition;



public interface RemoteTransitionCompat {

    RemoteTransition getRemoteTransition(
            @NonNull IRemoteTransition remoteTransition,
            @Nullable IApplicationThread appThread,
            @Nullable String debugName);
}
