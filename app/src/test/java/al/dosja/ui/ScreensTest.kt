package al.dosja.ui

import al.dosja.data.SampleCases
import al.dosja.inference.ConsentReason
import al.dosja.inference.Outcome
import al.dosja.inference.Source
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Renders the app's own composables on the JVM (Paparazzi, layoutlib) so the
 * submission has screenshots and video frames even without an attached Android
 * device. This is a render of the real Compose layout, not a device capture —
 * the README says so as well.
 */
class ScreensTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun document_list() {
        paparazzi.snapshot {
            Frame {
                DocumentList(
                    docs = SampleCases.all,
                    policyLine = "cloud allowed: true · consent required: true · threshold 6000 chars"
                ) {}
            }
        }
    }

    @Test
    fun on_device_result() {
        paparazzi.snapshot {
            Frame {
                ResultCard(
                    Outcome(
                        summary = "• Rent is EUR 640 per month, due by the fifth (Art. 4.1).\n" +
                            "• The landlord may review the rent once every twelve months with sixty days' " +
                            "written notice (Art. 4.2).\n" +
                            "• Any review is capped by the official consumer price index (Art. 4.3).",
                        source = Source.ON_DEVICE,
                        note = "On-device (Gemini Nano · ML Kit GenAI)"
                    )
                )
            }
        }
    }

    @Test
    fun cloud_result() {
        paparazzi.snapshot {
            Frame {
                ResultCard(
                    Outcome(
                        summary = "120 clauses recorded. The claimant notified the respondent in writing " +
                            "before each phase; annexes 1–120 support each notification.",
                        source = Source.CLOUD,
                        note = "Cloud (Firebase AI Logic · gemini-2.5-flash) — sent with your consent"
                    )
                )
            }
        }
    }

    @Test
    fun consent_dialog() {
        paparazzi.snapshot {
            Frame {
                ConsentDialog(
                    state = ScreenState.Consent(ConsentReason.TOO_LONG_FOR_DEVICE, 14_820),
                    onSend = {},
                    onStay = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Frame(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Scaffold(topBar = { TopAppBar(title = { Text("Dosja") }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                content()
                androidx.compose.foundation.layout.Spacer(Modifier.padding(2.dp))
            }
        }
    }
}
