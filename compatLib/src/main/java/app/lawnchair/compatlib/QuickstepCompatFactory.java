package app.lawnchair.compatlib;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;



public interface QuickstepCompatFactory {

    @NonNull
    ActivityManagerCompat getActivityManagerCompat();

    @NonNull
    ActivityOptionsCompat getActivityOptionsCompat();

    @NonNull
    RemoteTransitionCompat getRemoteTransitionCompat();
}
