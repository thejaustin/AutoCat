package app.lawnchair.root;

interface IRootHelper {
    oneway void goToSleep();
    String archivePackage(String packageName);
    String uninstallPackageKeepData(String packageName);
}
