package al.dosja.data

import al.dosja.inference.RoutingPolicy
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

/**
 * Where the routing policy comes from.
 *
 * A law office with twenty phones should not configure the same threshold twenty
 * times, so the policy is read from Firebase Remote Config with safe local
 * defaults. If Firebase is not configured in this build, or the fetch fails, the
 * defaults below are used — never a more permissive policy than the owner chose.
 */
class PolicySource(private val context: Context) {

    fun load(): RoutingPolicy {
        if (!hasFirebase()) return DEFAULT

        return try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = 600
                }
            )
            remoteConfig.setDefaultsAsync(
                mapOf(
                    KEY_THRESHOLD to DEFAULT.cloudThresholdChars.toLong(),
                    KEY_ALLOW_CLOUD to DEFAULT.allowCloud,
                    KEY_REQUIRE_CONSENT to DEFAULT.requireConsentForCloud
                )
            )
            remoteConfig.fetchAndActivate()
            RoutingPolicy(
                cloudThresholdChars = remoteConfig.getLong(KEY_THRESHOLD).toInt(),
                allowCloud = remoteConfig.getBoolean(KEY_ALLOW_CLOUD),
                requireConsentForCloud = remoteConfig.getBoolean(KEY_REQUIRE_CONSENT)
            )
        } catch (t: Throwable) {
            Log.w("Dosja", "Remote Config unavailable, using local defaults", t)
            DEFAULT
        }
    }

    private fun hasFirebase(): Boolean = try {
        FirebaseApp.getApps(context).isNotEmpty()
    } catch (t: Throwable) {
        false
    }

    companion object {
        const val KEY_THRESHOLD = "cloud_threshold_chars"
        const val KEY_ALLOW_CLOUD = "allow_cloud"
        const val KEY_REQUIRE_CONSENT = "require_consent_for_cloud"

        val DEFAULT = RoutingPolicy(
            cloudThresholdChars = 6_000,
            allowCloud = true,
            requireConsentForCloud = true
        )
    }
}
