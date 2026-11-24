package app.lawnchair.backend

/** A way AutoCat can reach privileged system functionality. Ordered as in Automatic mode. */
enum class Backend {
    Root,

    /** Our own Shizuku fork, detected through its custom binder transaction. */
    ShizukuPlus,

    /** Stock Shizuku (ShizukuPlus is wire-compatible and also satisfies this). */
    Shizuku,

    /** Consumer-only: never provisions a device owner. Currently a detection stub. */
    Dhizuku,
    Accessibility,
    ;

    /** True for the backends that execute through the Shizuku binder. */
    val isShizukuFamily get() = this == Shizuku || this == ShizukuPlus

    /** Whether this backend can ever serve [capability]. */
    fun supports(capability: Capability): Boolean = this in capability.supportedBackends
}

enum class Capability(val supportedBackends: List<Backend>) {
    /** Reading and controlling recent tasks (needs an IUserService host). */
    TaskControl(listOf(Backend.Root, Backend.ShizukuPlus, Backend.Shizuku)),

    /** Turning the screen off. */
    ScreenLock(listOf(Backend.Root, Backend.ShizukuPlus, Backend.Shizuku, Backend.Dhizuku, Backend.Accessibility)),

    /** Archive, disable, enable and install packages through `pm`. */
    AppState(listOf(Backend.Root, Backend.ShizukuPlus, Backend.Shizuku)),
}

/** The user's choice of backend: follow the capability order, or use exactly one. */
enum class BackendMode(val pinned: Backend?) {
    Automatic(null),
    Root(Backend.Root),
    ShizukuPlus(Backend.ShizukuPlus),
    Shizuku(Backend.Shizuku),
    Dhizuku(Backend.Dhizuku),
    Accessibility(Backend.Accessibility),
    ;

    companion object {
        fun fromString(value: String) = entries.firstOrNull { it.name == value } ?: Automatic
    }
}

sealed interface BackendStatus {
    data object Available : BackendStatus
    data object Unavailable : BackendStatus

    /** Present on the device but AutoCat can't use it yet (Dhizuku stub, or Plus without permission). */
    data object Detected : BackendStatus
}

/** Outcome of resolving a capability: usable backends in order, or why there are none. */
data class Resolution(
    val capability: Capability,
    val backends: List<Backend>,
    /** The pinned backend that was asked for but can't be used, if any. */
    val pinnedUnavailable: Backend? = null,
) {
    val isEmpty get() = backends.isEmpty()

    fun failureReason(): String = if (pinnedUnavailable != null) {
        "${pinnedUnavailable.name} is selected in Privileged access but isn't available"
    } else {
        "No privileged backend is available for ${capability.name}"
    }
}
