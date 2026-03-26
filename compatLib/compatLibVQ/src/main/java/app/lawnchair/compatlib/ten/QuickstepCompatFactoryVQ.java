package app.lawnchair.compatlib.ten;

import android.window.RemoteTransition;
import androidx.annotation.RequiresApi;
import app.lawnchair.compatlib.ActivityManagerCompat;
import app.lawnchair.compatlib.ActivityOptionsCompat;
import app.lawnchair.compatlib.QuickstepCompatFactory;
import app.lawnchair.compatlib.RemoteTransitionCompat;

@RequiresApi(29)
public class QuickstepCompatFactoryVQ implements QuickstepCompatFactory {
    protected final String TAG = getClass().getCanonicalName();

    @Override
    public ActivityManagerCompat getActivityManagerCompat() {
        return new ActivityManagerCompatVQ();
    }

    @Override
    public ActivityOptionsCompat getActivityOptionsCompat() {
        return new ActivityOptionsCompatVQ();
    }

    @Override
    public RemoteTransitionCompat getRemoteTransitionCompat() {
        return RemoteTransition::new;
    }
}
