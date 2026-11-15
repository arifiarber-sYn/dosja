package al.dosja.inference

/**
 * The contract both halves of the hybrid pipeline implement.
 *
 * Nothing in here knows whether inference happens on the phone or in a data
 * centre: that decision belongs to [RoutingPolicy] and [InferenceRouter], which
 * are pure Kotlin so the policy can be tested without an Android device.
 */
interface InferenceEngine {

    /** Stable id used in the UI badge and in the audit trail. */
    val id: String

    /** Whether this engine can serve a request right now (model present, config present). */
    suspend fun isAvailable(): Boolean

    /** Summarise [text]. Must never be called for cloud work without user consent. */
    suspend fun summarize(text: String): EngineResult
}

sealed interface EngineResult {

    data class Ok(val text: String) : EngineResult

    /** The on-device model refused this input (too long, language, unsupported feature). */
    data class Declined(val reason: String) : EngineResult

    /** The engine is not usable on this device or has no configuration. */
    data class Unavailable(val reason: String) : EngineResult

    data class Error(val message: String) : EngineResult
}

/**
 * The routing policy. Defaults are safe: consent is required before any byte
 * leaves the device, and the cloud can be switched off entirely by policy.
 */
data class RoutingPolicy(
    val cloudThresholdChars: Int = 6_000,
    val allowCloud: Boolean = true,
    val requireConsentForCloud: Boolean = true
)

data class Outcome(
    val summary: String,
    val source: Source,
    val note: String? = null
)

enum class Source { ON_DEVICE, CLOUD, DEMO }

/** Why the router stopped and asked a human. */
enum class ConsentReason { TOO_LONG_FOR_DEVICE, DEVICE_DECLINED }

sealed interface RoutingResult {

    data class Done(val outcome: Outcome) : RoutingResult

    /** The request is queued; nothing has left the device yet. */
    data class NeedsConsent(val reason: ConsentReason, val chars: Int) : RoutingResult

    data class Failed(val message: String) : RoutingResult
}
