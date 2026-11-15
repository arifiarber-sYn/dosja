package al.dosja.inference

/**
 * Demo-only engine.
 *
 * It produces a scripted summary so the routing UI can be demonstrated on a
 * device without AICore or without a Firebase project. Results from this engine
 * are always labelled DEMO in the interface and are never presented as model
 * output.
 */
class ScriptedEngine : InferenceEngine {

    override val id: String = "scripted demo"

    override suspend fun isAvailable(): Boolean = true

    override suspend fun summarize(text: String): EngineResult = EngineResult.Ok(
        "Scripted demo summary (no model was used). " +
            "The document has ${text.length} characters. " +
            "On a device with AICore this card would show Gemini Nano output, " +
            "and a long document would instead ask for consent to use the cloud."
    )
}
