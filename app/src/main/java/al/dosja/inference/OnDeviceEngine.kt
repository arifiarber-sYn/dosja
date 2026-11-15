package al.dosja.inference

import android.content.Context
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import kotlinx.coroutines.guava.await

/**
 * On-device half of the pipeline: Gemini Nano through ML Kit GenAI (AICore).
 *
 * No network call is made here. If AICore is missing or the feature is still
 * downloading, the engine reports Unavailable and the router decides what to do
 * next — the engine never escalates on its own.
 */
class OnDeviceEngine(private val context: Context) : InferenceEngine {

    override val id: String = "Gemini Nano (ML Kit GenAI)"

    private val summarizer: Summarizer by lazy {
        Summarization.getClient(
            SummarizerOptions.builder(context)
                .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
                .build()
        )
    }

    override suspend fun isAvailable(): Boolean = try {
        summarizer.checkFeatureStatus().await() == FeatureStatus.AVAILABLE
    } catch (t: Throwable) {
        false
    }

    override suspend fun summarize(text: String): EngineResult {
        return try {
            val status = summarizer.checkFeatureStatus().await()
            when {
                status == FeatureStatus.UNAVAILABLE ->
                    EngineResult.Unavailable("AICore is not available on this device")
                status == FeatureStatus.DOWNLOADABLE ->
                    EngineResult.Unavailable("the on-device model is not downloaded yet")
                status == FeatureStatus.DOWNLOADING ->
                    EngineResult.Unavailable("the on-device model is still downloading")
                else -> {
                    val request = SummarizationRequest.builder(text).build()
                    val summary = summarizer.runInference(request).await().summary.trim()
                    if (summary.isEmpty()) EngineResult.Declined("empty summary from the on-device model")
                    else EngineResult.Ok(summary)
                }
            }
        } catch (t: Throwable) {
            // GenAiException, unsupported language, input too long: all become Declined
            // so the router can ask the user instead of guessing.
            EngineResult.Declined(t.message ?: "on-device inference failed")
        }
    }
}
