package al.dosja.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Syncs case *metadata* between devices — never document bodies.
 *
 * That split is the product decision: the same case opens on a second phone, but
 * the file itself still lives on the phone it was scanned on.
 */
class CaseMetadataStore(private val context: Context) {

    fun publish(doc: CaseDoc, source: String, decidedBy: String) {
        if (!hasFirebase()) return
        try {
            FirebaseFirestore.getInstance()
                .collection("cases")
                .document(doc.id)
                .set(
                    mapOf(
                        "title" to doc.title,
                        "kind" to doc.kind,
                        "lastDecision" to source,
                        "decidedBy" to decidedBy,
                        "chars" to doc.text.length,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
        } catch (t: Throwable) {
            Log.w("Dosja", "metadata sync skipped", t)
        }
    }

    private fun hasFirebase(): Boolean = try {
        FirebaseApp.getApps(context).isNotEmpty()
    } catch (t: Throwable) {
        false
    }
}
