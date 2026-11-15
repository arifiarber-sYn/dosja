package al.dosja.inference

/**
 * The single routing decision in the app.
 *
 * Order of operations, and the reason it is written this way:
 *  1. Short documents are always attempted on-device first — that is the point.
 *  2. If the device declines, the request is **not** silently escalated. It is
 *     turned into a [RoutingResult.NeedsConsent] so a human sees exactly what
 *     would be sent and to whom.
 *  3. Cloud work happens only when policy allows it and consent is present.
 */
class InferenceRouter(
    private val onDevice: InferenceEngine,
    private val cloud: InferenceEngine,
    private val policy: RoutingPolicy
) {

    suspend fun summarize(text: String, consentGiven: Boolean = false): RoutingResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return RoutingResult.Failed("Document is empty.")

        val tooLong = trimmed.length > policy.cloudThresholdChars

        if (!tooLong) {
            when (val local = onDevice.summarize(trimmed)) {
                is EngineResult.Ok -> return RoutingResult.Done(
                    Outcome(local.text, Source.ON_DEVICE, "On-device (${onDevice.id})")
                )
                is EngineResult.Unavailable -> Unit // fall through to the cloud decision
                is EngineResult.Declined -> Unit
                is EngineResult.Error -> Unit
            }
        }

        val reason = if (tooLong) ConsentReason.TOO_LONG_FOR_DEVICE else ConsentReason.DEVICE_DECLINED

        if (!policy.allowCloud) {
            return RoutingResult.Failed(
                "The on-device model could not handle this document and cloud routing is disabled by policy. Nothing was sent."
            )
        }

        if (policy.requireConsentForCloud && !consentGiven) {
            return RoutingResult.NeedsConsent(reason, trimmed.length)
        }

        return when (val remote = cloud.summarize(trimmed)) {
            is EngineResult.Ok -> RoutingResult.Done(
                Outcome(remote.text, Source.CLOUD, "Cloud (${cloud.id}) — sent with your consent")
            )
            is EngineResult.Unavailable -> RoutingResult.Failed(
                "Cloud is not configured on this build: ${remote.reason}"
            )
            is EngineResult.Declined -> RoutingResult.Failed("Cloud declined: ${remote.reason}")
            is EngineResult.Error -> RoutingResult.Failed("Cloud error: ${remote.message}")
        }
    }
}
