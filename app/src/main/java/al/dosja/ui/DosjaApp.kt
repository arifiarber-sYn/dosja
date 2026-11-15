package al.dosja.ui

import al.dosja.data.CaseDoc
import al.dosja.inference.ConsentReason
import al.dosja.inference.Outcome
import al.dosja.inference.Source
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DosjaApp(vm: DosjaViewModel = viewModel()) {
    MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dosja", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("demo", fontSize = 12.sp)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Switch(checked = vm.demoMode, onCheckedChange = { vm.toggleDemo() })
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                val doc = vm.selected
                if (doc == null) DocumentList(vm.documents, vm.policy.let { "cloud allowed: ${it.allowCloud} · consent: ${it.requireConsentForCloud} · threshold ${it.cloudThresholdChars} chars" }) { vm.open(it) }
                else DocumentDetail(vm, doc)
            }
        }

        when (val s = vm.state) {
            is ScreenState.Consent -> ConsentDialog(s, onSend = { vm.approveCloud() }, onStay = { vm.dismissConsent() })
            else -> Unit
        }
    }
}

@Composable
internal fun DocumentList(docs: List<CaseDoc>, policyLine: String, onOpen: (CaseDoc) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Case documents", style = MaterialTheme.typography.titleMedium)
        Text(
            "Nothing leaves this phone unless you say so.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF555555)
        )
        Spacer(Modifier.height(4.dp))
        Text(policyLine, fontSize = 11.sp, color = Color(0xFF777777))
        Spacer(Modifier.height(16.dp))
        docs.forEach { doc ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpen(doc) },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(doc.title, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("${doc.kind} · ${doc.text.length} chars", fontSize = 12.sp, color = Color(0xFF666666))
                }
            }
        }
    }
}

@Composable
private fun DocumentDetail(vm: DosjaViewModel, doc: CaseDoc) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        TextButton(onClick = { vm.back() }) { Text("← Back") }
        Text(doc.title, style = MaterialTheme.typography.titleLarge)
        Text(doc.kind, fontSize = 12.sp, color = Color(0xFF666666))
        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Text(
                doc.text.take(700) + if (doc.text.length > 700) "\n…" else "",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.summarize() },
            enabled = vm.state !is ScreenState.Running,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.state is ScreenState.Running) "Working…" else "Summarise")
        }

        Spacer(Modifier.height(16.dp))
        when (val s = vm.state) {
            is ScreenState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.height(18.dp))
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text("Routing…", fontSize = 13.sp)
            }
            is ScreenState.Done -> ResultCard(s.outcome)
            is ScreenState.Failed -> Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) { Text(s.message, Modifier.padding(12.dp), fontSize = 13.sp) }
            is ScreenState.Consent -> Text("Waiting for your decision…", fontSize = 13.sp)
            ScreenState.Idle -> Text(
                "Short documents are summarised on-device. Long ones stop and ask.",
                fontSize = 12.sp,
                color = Color(0xFF777777)
            )
        }
    }
}

@Composable
internal fun ResultCard(outcome: Outcome) {
    val (label, colour) = when (outcome.source) {
        Source.ON_DEVICE -> "ON-DEVICE" to Color(0xFF1B5E20)
        Source.CLOUD -> "CLOUD (with consent)" to Color(0xFF0D47A1)
        Source.DEMO -> "DEMO — no model" to Color(0xFF6A1B9A)
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(colour, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(outcome.summary, fontSize = 13.sp)
            outcome.note?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, fontSize = 11.sp, color = Color(0xFF666666))
            }
        }
    }
}

@Composable
internal fun ConsentDialog(state: ScreenState.Consent, onSend: () -> Unit, onStay: () -> Unit) {
    val why = when (state.reason) {
        ConsentReason.TOO_LONG_FOR_DEVICE ->
            "This document is ${state.chars} characters — longer than the on-device limit. The on-device model did not read it."
        ConsentReason.DEVICE_DECLINED ->
            "The on-device model declined this document, so it has not been read yet."
    }
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text("Send this document to the cloud?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(why, fontSize = 13.sp)
                Text(
                    "If you agree, the text is sent to Gemini through Firebase AI Logic. Nothing has been sent so far.",
                    fontSize = 13.sp
                )
                Text("Refusing keeps the document on this phone.", fontSize = 12.sp, color = Color(0xFF666666))
            }
        },
        confirmButton = { Button(onClick = onSend) { Text("Send to cloud") } },
        dismissButton = { TextButton(onClick = onStay) { Text("Keep on device") } }
    )
}
