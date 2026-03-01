package app.lawnchair.compatlib;



public interface QuickstepCompatFactory {

    androidx.annotation.NonNull
    ActivityManagerCompat getActivityManagerCompat();

    androidx.annotation.NonNull
    ActivityOptionsCompat getActivityOptionsCompat();

    androidx.annotation.NonNull
    RemoteTransitionCompat getRemoteTransitionCompat();
}
