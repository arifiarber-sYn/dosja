package al.dosja.inference

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend

/**
 * Cloud half of the pipeline: Firebase AI Logic (Gemini).
 *
 * Only ever reached through [InferenceRouter] after an explicit consent step.
 * App Check (see MainActivity) gates the calls so only our app instances can
 * spend the project's quota.
 *
 * A build without Firebase configuration reports Unavailable instead of
 * crashing: an app whose cloud half is missing should say so honestly.
 */
class CloudEngine(
    private val context: Context,
    private val modelName: String = "gemini-2.5-flash"
) : InferenceEngine {

    override val id: String = "Firebase AI Logic ($modelName)"

    override suspend fun isAvailable(): Boolean = hasFirebase()

    private fun hasFirebase(): Boolean = try {
        FirebaseApp.getApps(context).isNotEmpty()
    } catch (t: Throwable) {
        false
    }

    override suspend fun summarize(text: String): EngineResult {
        if (!hasFirebase()) {
            return EngineResult.Unavailable("no Firebase configuration in this build")
        }
        return try {
            val model = FirebaseAI.getInstance(
                FirebaseApp.getInstance(),
                GenerativeBackend.googleAI()
            ).generativeModel(modelName)
            val response = model.generateContent("$SYSTEM\n\n$text")
            val summary = response.text?.trim().orEmpty()
            if (summary.isEmpty()) EngineResult.Declined("empty cloud response")
            else EngineResult.Ok(summary)
        } catch (t: Throwable) {
            EngineResult.Error(t.message ?: "cloud call failed")
        }
    }

    companion object {
        const val SYSTEM =
            "You summarise legal documents for a professional reader. " +
                "Answer in the language of the document, cite clause numbers when present, " +
                "and never invent facts, dates, amounts or clause numbers. " +
                "If something is unclear, say so instead of guessing."
    }
}
