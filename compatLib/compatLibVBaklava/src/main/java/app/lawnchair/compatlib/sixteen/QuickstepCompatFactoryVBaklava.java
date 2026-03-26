package app.lawnchair.compatlib.sixteen;

import android.window.RemoteTransition;
import androidx.annotation.RequiresApi;
import app.lawnchair.compatlib.ActivityManagerCompat;
import app.lawnchair.compatlib.ActivityOptionsCompat;
import app.lawnchair.compatlib.RemoteTransitionCompat;
import app.lawnchair.compatlib.fifteen.QuickstepCompatFactoryVV;

@RequiresApi(36)
public class QuickstepCompatFactoryVBaklava extends QuickstepCompatFactoryVV {

    @Override
    public ActivityManagerCompat getActivityManagerCompat() {
        return new ActivityManagerCompatVBaklava();
    }

    @Override
    public ActivityOptionsCompat getActivityOptionsCompat() {
        return new ActivityOptionsCompatVBaklava();
    }

    @Override
    public RemoteTransitionCompat getRemoteTransitionCompat() {
        return RemoteTransition::new;
    }
}
