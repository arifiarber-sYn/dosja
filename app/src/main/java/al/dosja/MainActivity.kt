package al.dosja

import al.dosja.ui.DosjaApp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureFirebase()
        setContent { DosjaApp() }
    }

    /**
     * Firebase is optional in this build: without google-services.json the app
     * still runs, and the cloud half of the pipeline reports itself unavailable.
     * When a project *is* configured, App Check is installed before any call so
     * that cloud quota can only be spent by verified instances of this app.
     */
    private fun configureFirebase() {
        try {
            val app = FirebaseApp.initializeApp(this) ?: return
            if (app.name.isNotEmpty()) {
                FirebaseAppCheck.getInstance()
                    .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
        } catch (t: Throwable) {
            // No configuration in this build — on-device routing is unaffected.
        }
    }
}
