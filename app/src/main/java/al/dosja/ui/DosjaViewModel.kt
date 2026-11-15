package al.dosja.ui

import al.dosja.data.CaseDoc
import al.dosja.data.CaseMetadataStore
import al.dosja.data.PolicySource
import al.dosja.data.SampleCases
import al.dosja.inference.CloudEngine
import al.dosja.inference.ConsentReason
import al.dosja.inference.InferenceRouter
import al.dosja.inference.OnDeviceEngine
import al.dosja.inference.Outcome
import al.dosja.inference.RoutingPolicy
import al.dosja.inference.RoutingResult
import al.dosja.inference.ScriptedEngine
import al.dosja.inference.Source
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

sealed interface ScreenState {
    data object Idle : ScreenState
    data object Running : ScreenState
    data class Consent(val reason: ConsentReason, val chars: Int) : ScreenState
    data class Done(val outcome: Outcome) : ScreenState
    data class Failed(val message: String) : ScreenState
}

class DosjaViewModel(app: Application) : AndroidViewModel(app) {

    private val policySource = PolicySource(app)
    private val metadata = CaseMetadataStore(app)
    private val demoEngine = ScriptedEngine()
    private val onDeviceEngine = OnDeviceEngine(app)
    private val cloudEngine = CloudEngine(app)

    var policy: RoutingPolicy by mutableStateOf(policySource.load())
        private set

    var demoMode: Boolean by mutableStateOf(false)
        private set

    var selected: CaseDoc? by mutableStateOf(null)
        private set

    var state: ScreenState by mutableStateOf(ScreenState.Idle)
        private set

    val documents: List<CaseDoc> = SampleCases.all

    fun open(doc: CaseDoc) {
        selected = doc
        state = ScreenState.Idle
    }

    fun back() {
        selected = null
        state = ScreenState.Idle
    }

    fun toggleDemo() {
        demoMode = !demoMode
    }

    fun refreshPolicy() {
        policy = policySource.load()
    }

    fun summarize() {
        val doc = selected ?: return
        state = ScreenState.Running
        viewModelScope.launch {
            if (demoMode) {
                val result = demoEngine.summarize(doc.text)
                state = ScreenState.Done(Outcome(demoText(result), Source.DEMO, "Scripted demo — no model was used"))
                return@launch
            }
            val router = InferenceRouter(onDeviceEngine, cloudEngine, policy)
            when (val result = router.summarize(doc.text, consentGiven = false)) {
                is RoutingResult.Done -> complete(result.outcome)
                is RoutingResult.NeedsConsent -> state = ScreenState.Consent(result.reason, result.chars)
                is RoutingResult.Failed -> state = ScreenState.Failed(result.message)
            }
        }
    }

    fun dismissConsent() {
        state = ScreenState.Idle
    }

    fun approveCloud() {
        val doc = selected ?: return
        state = ScreenState.Running
        viewModelScope.launch {
            val router = InferenceRouter(onDeviceEngine, cloudEngine, policy)
            when (val result = router.summarize(doc.text, consentGiven = true)) {
                is RoutingResult.Done -> complete(result.outcome)
                is RoutingResult.NeedsConsent -> state = ScreenState.Failed("Consent was not applied.")
                is RoutingResult.Failed -> state = ScreenState.Failed(result.message)
            }
        }
    }

    private fun complete(outcome: Outcome) {
        state = ScreenState.Done(outcome)
        selected?.let { doc ->
            metadata.publish(doc, outcome.source.name, decidedBy = "user")
        }
    }

    private fun demoText(result: al.dosja.inference.EngineResult): String =
        (result as? al.dosja.inference.EngineResult.Ok)?.text ?: "demo"
}
