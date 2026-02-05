package app.lawnchair.appops

/**
 * Represents the available methods for archiving an app,
 * ordered by preference (most desirable first).
 */
sealed class ArchiveMethod {
    /** Android 15+ PackageInstaller.requestArchive() — requires installer-of-record */
    data object Api35Archive : ArchiveMethod()

    /** Root shell: `pm archive <package>` (Android 15+ with root) */
    data object RootArchive : ArchiveMethod()

    /** Shizuku shell: `pm uninstall -k <package>` or `pm archive` */
    data object ShizukuArchive : ArchiveMethod()

    /** Root shell: `pm uninstall -k <package>` (keeps data, works on older Android) */
    data object RootUninstallKeepData : ArchiveMethod()

    /** Launch system uninstall intent per-app (user confirms each) */
    data object IntentFallback : ArchiveMethod()
}
