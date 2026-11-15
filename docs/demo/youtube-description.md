Dosja — private legal document work on your phone. Google #BuildHybridAI 2026 (Android).

A case file is the most confidential document a person ever hands to a lawyer. Most AI tools that
"summarise this contract" upload that file to a data centre in another country without asking.
Dosja is built the other way round: the on-device model (Gemini Nano via ML Kit GenAI / AICore) runs
first, with no network at all. Cloud (Firebase AI Logic, gemini-2.5-flash) is used only for long
documents, only after an explicit dialog that states the document size and the destination, and
"Keep on device" is always one tap away. If Remote Config sets allow_cloud=false, nothing leaves the
phone at all.

What this demo shows:
- the case list and an on-device summary (no network)
- a long document where the on-device model declines and the app stops and asks
- the consent dialog: 14820 characters, destination named, refusal keeps the file local
- the cloud result after consent
- the decision logic itself: InferenceRouter is plain Kotlin, covered by unit tests

Honest note on how this video was captured: the Android emulator does not start on this machine
(qemu segfaults) and no phone was attached, so the screens are rendered from the app's real Compose
layouts with Paparazzi (layoutlib) rather than filmed on a device. The UI you see is the app's own
composable code, not a mockup; the device-level behaviour is covered by the unit tests and the
declared source, and is stated plainly in the repository README as well.

Source (MIT): https://github.com/arifiarber-sYn/dosja
Built with: Kotlin, Jetpack Compose, ML Kit GenAI (Gemini Nano), Firebase AI Logic, Remote Config,
App Check (Play Integrity), Firestore (case metadata only).

Chapters (measured from the actual scene cuts, 14.9 / 25.7 / 33.6 / 46.6 / 59.1 s):
0:00 what Dosja is
0:14 every document starts on the device
0:33 long documents stop and ask for consent
0:46 the cloud answer, only after that consent
0:59 the routing policy, and where the code is
