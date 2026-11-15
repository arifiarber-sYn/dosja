# Dosja

**Private legal document work on your phone: on-device Gemini Nano first, cloud only when you allow it.**

Submission for [Google #BuildHybridAI 2026](https://firebasehybridaihackathon.devpost.com/) — Android track.

---

## The idea

A case file is the most confidential document a person ever hands to a lawyer. Every AI tool that
promises to "summarise this contract" quietly uploads that file to a data centre in another country.
For a small law office that is not a technicality — it is the reason they cannot use AI at all.

Dosja starts from the opposite rule: **the file does not leave the phone unless a human says so.**

## What it does

* Summarises legal documents **on-device** with Gemini Nano through ML Kit GenAI (AICore). No network call.
* When the on-device model declines or the document is longer than the configured threshold, the request is
  **not** silently escalated. It stops, explains why, and shows a consent dialog with the character count.
  Only if the user taps *Send to cloud* does the text go to **Firebase AI Logic** (Gemini).
* **Firebase Remote Config** supplies the routing policy (`cloud_threshold_chars`, `allow_cloud`,
  `require_consent_for_cloud`) so an office can tune it once for twenty phones instead of twenty times.
  Safe local defaults apply when Remote Config is unreachable — never a more permissive policy than the owner chose.
* **Firebase App Check** (Play Integrity) gates every cloud call so only verified instances of this app
  can spend the project's quota.
* **Cloud Firestore** syncs case **metadata** only (title, kind, last decision) — never document bodies.
* Every result card is labelled `ON-DEVICE`, `CLOUD (with consent)` or `DEMO — no model`, so the reader
  always knows where the text went.

## Architecture

```
app/src/main/java/al/dosja/
├── inference/
│   ├── Inference.kt            # InferenceEngine contract, EngineResult, RoutingPolicy, Outcome
│   ├── InferenceRouter.kt      # THE routing decision — pure Kotlin, unit-tested
│   ├── OnDeviceEngine.kt       # Gemini Nano via ML Kit GenAI (AICore)
│   ├── CloudEngine.kt          # Firebase AI Logic (Gemini)
│   └── ScriptedEngine.kt       # demo-only engine, always labelled DEMO
├── data/
│   ├── PolicySource.kt         # Remote Config → RoutingPolicy (with safe defaults)
│   ├── CaseMetadataStore.kt    # Firestore, metadata only
│   └── SampleCases.kt          # three documents to try the app with
├── ui/                         # Compose screens + ViewModel
└── MainActivity.kt             # Firebase/App Check bootstrap
```

The routing decision is a single function so the policy can be tested without a device:

```kotlin
val router = InferenceRouter(onDeviceEngine, cloudEngine, policy)
when (val result = router.summarize(text, consentGiven = false)) {
    is RoutingResult.Done        -> show(result.outcome)          // source is labelled
    is RoutingResult.NeedsConsent -> askHuman(result.reason, result.chars)
    is RoutingResult.Failed       -> show(result.message)         // nothing was sent
}
```

## Build

```bash
export JAVA_HOME=/path/to/jdk21
./gradlew :app:assembleDebug     # or: gradle :app:assembleDebug
./gradlew :app:testDebugUnitTest # 6 tests covering the routing policy
```

Requires Android SDK 35 and JDK 17–21.

### Optional: the cloud half

The app runs without Firebase. To enable the cloud path, add your own `google-services.json` from a
Firebase project with **AI Logic** and **App Check** (Play Integrity) enabled, plus Remote Config
parameters `cloud_threshold_chars` (number), `allow_cloud` (boolean), `require_consent_for_cloud` (boolean).
Without it, `CloudEngine` reports `Unavailable` and the UI says so instead of failing silently.

## Status — what is verified and what is not

Verified in this repository:

* `:app:assembleDebug` builds a debug APK.
* `:app:testDebugUnitTest` — 6 tests, all passing: on-device first, long documents require consent,
  declared consent reaches the cloud, device declination also asks, a policy forbidding cloud sends
  nothing at all, empty documents fail fast.

Not verified here (honest limits):

* **On-device inference was not exercised on a device with AICore during development** — the code path is
  real, but a run needs a phone whose AICore has the summarisation model available. The app reports the
  exact `FeatureStatus` when it does not.
* **Cloud inference needs a Firebase project** with AI Logic enabled; no project credentials are committed.
  The `DEMO` toggle in the top bar exists so the routing UI can be demonstrated without either.

## License

MIT — see [LICENSE](LICENSE).
