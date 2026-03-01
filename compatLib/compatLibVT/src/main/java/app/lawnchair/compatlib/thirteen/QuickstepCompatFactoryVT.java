package app.lawnchair.compatlib.thirteen;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.window.RemoteTransition;

import androidx.annotation.RequiresApi;
import app.lawnchair.compatlib.ActivityManagerCompat;
import app.lawnchair.compatlib.ActivityOptionsCompat;
import app.lawnchair.compatlib.RemoteTransitionCompat;
import app.lawnchair.compatlib.twelve.QuickstepCompatFactoryVS;

@RequiresApi(33)
public class QuickstepCompatFactoryVT extends QuickstepCompatFactoryVS {

    @Override
    public ActivityManagerCompat getActivityManagerCompat() {
        return new ActivityManagerCompatVT();
    }

    @Override
    public ActivityOptionsCompat getActivityOptionsCompat() {
        return new ActivityOptionsCompatVT();
    }

    @Override
    public RemoteTransitionCompat getRemoteTransitionCompat() {
        return (remoteTransition, appThread, debugName) ->
                new RemoteTransition(remoteTransition, appThread);
    }
}
