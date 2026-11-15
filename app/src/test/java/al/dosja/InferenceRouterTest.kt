package al.dosja

import al.dosja.inference.ConsentReason
import al.dosja.inference.EngineResult
import al.dosja.inference.InferenceEngine
import al.dosja.inference.InferenceRouter
import al.dosja.inference.RoutingPolicy
import al.dosja.inference.RoutingResult
import al.dosja.inference.Source
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeEngine(
    override val id: String,
    private val result: EngineResult,
    private var calls: Int = 0
) : InferenceEngine {
    override suspend fun isAvailable(): Boolean = result !is EngineResult.Unavailable
    override suspend fun summarize(text: String): EngineResult {
        calls++
        return result
    }
    fun callCount() = calls
}

class InferenceRouterTest {

    private val ok = EngineResult.Ok("summary")
    private val short = "Article 4 — rent is EUR 640 per month."

    @Test
    fun `short document runs on device and never reaches the cloud`() = runTest {
        val cloud = FakeEngine("cloud", ok)
        val router = InferenceRouter(FakeEngine("nano", ok), cloud, RoutingPolicy())
        val result = router.summarize(short, consentGiven = false)
        assertTrue(result is RoutingResult.Done)
        assertEquals(Source.ON_DEVICE, (result as RoutingResult.Done).outcome.source)
        assertEquals(0, cloud.callCount())
    }

    @Test
    fun `long document stops and asks for consent instead of escalating`() = runTest {
        val cloud = FakeEngine("cloud", ok)
        val router = InferenceRouter(FakeEngine("nano", ok), cloud, RoutingPolicy(cloudThresholdChars = 100))
        val result = router.summarize("x".repeat(500), consentGiven = false)
        assertTrue(result is RoutingResult.NeedsConsent)
        assertEquals(ConsentReason.TOO_LONG_FOR_DEVICE, (result as RoutingResult.NeedsConsent).reason)
        assertEquals(0, cloud.callCount())
    }

    @Test
    fun `consent sends the long document to the cloud and labels the source`() = runTest {
        val router = InferenceRouter(FakeEngine("nano", ok), FakeEngine("cloud", ok), RoutingPolicy(cloudThresholdChars = 100))
        val result = router.summarize("x".repeat(500), consentGiven = true)
        assertTrue(result is RoutingResult.Done)
        assertEquals(Source.CLOUD, (result as RoutingResult.Done).outcome.source)
    }

    @Test
    fun `device declination asks for consent too`() = runTest {
        val declined = FakeEngine("nano", EngineResult.Declined("language not supported"))
        val router = InferenceRouter(declined, FakeEngine("cloud", ok), RoutingPolicy())
        val result = router.summarize(short, consentGiven = false)
        assertTrue(result is RoutingResult.NeedsConsent)
        assertEquals(ConsentReason.DEVICE_DECLINED, (result as RoutingResult.NeedsConsent).reason)
    }

    @Test
    fun `policy that forbids cloud never sends anything`() = runTest {
        val cloud = FakeEngine("cloud", ok)
        val router = InferenceRouter(
            FakeEngine("nano", EngineResult.Declined("too long")),
            cloud,
            RoutingPolicy(allowCloud = false)
        )
        val result = router.summarize(short, consentGiven = true)
        assertTrue(result is RoutingResult.Failed)
        assertEquals(0, cloud.callCount())
        assertTrue((result as RoutingResult.Failed).message.contains("Nothing was sent"))
    }

    @Test
    fun `empty document fails fast`() = runTest {
        val router = InferenceRouter(FakeEngine("nano", ok), FakeEngine("cloud", ok), RoutingPolicy())
        assertTrue(router.summarize("   ") is RoutingResult.Failed)
    }
}
