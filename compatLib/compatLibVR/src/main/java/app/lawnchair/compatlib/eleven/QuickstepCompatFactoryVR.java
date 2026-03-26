package app.lawnchair.compatlib.eleven;

import androidx.annotation.RequiresApi;
import app.lawnchair.compatlib.ActivityManagerCompat;
import app.lawnchair.compatlib.ActivityOptionsCompat;
import app.lawnchair.compatlib.ten.QuickstepCompatFactoryVQ;

@RequiresApi(30)
public class QuickstepCompatFactoryVR extends QuickstepCompatFactoryVQ {

    @Override
    public ActivityManagerCompat getActivityManagerCompat() {
        return new ActivityManagerCompatVR();
    }

    @Override
    public ActivityOptionsCompat getActivityOptionsCompat() {
        return new ActivityOptionsCompatVR();
    }
}
