package com.silomath.feature.masterdata.location

import com.silomath.feature.masterdata.location.map.*
import io.mockk.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class MapPrivacyTest {
    private val config=MapProviderConfig("https://tiles.example.test/{z}/{x}/{y}.png","Fixture provider","https://example.test/license")
    @Test fun onlyConfiguredHttpsNumericTilesArePermitted() {
        assertTrue(config.permits("https://tiles.example.test/12/34/56.png".toHttpUrl()))
        listOf("http://tiles.example.test/1/2/3.png","https://other.test/1/2/3.png",
            "https://tiles.example.test/1/2/3.png?token=secret","https://tiles.example.test/1/tenant/3.png",
            "https://tiles.example.test:444/1/2/3.png","https://user:pass@tiles.example.test/1/2/3.png").forEach {
            assertFalse(it,config.permits(it.toHttpUrl()))
        }
        assertFalse(MapProviderConfig().enabled)
        assertFalse(config.copy(tileTemplate="https://tiles.example.test/static.png").enabled)
    }
    @Test fun siloCredentialsHeadersAndBusinessBodyNeverReachProvider() {
        val chain=mockk<Interceptor.Chain>()
        val original=Request.Builder().url("https://tiles.example.test/1/2/3.png")
            .header("Authorization","Bearer silo-token").header("Cookie","refresh=secret")
            .header("X-Organization-Id","fixture-tenant").header("X-Request-Id","business-id")
            .post(RequestBody.create(null,"private business payload")).build()
        every{chain.request()} returns original
        val clean=slot<Request>()
        every{chain.proceed(capture(clean))} answers {
            Response.Builder().request(clean.captured).protocol(Protocol.HTTP_1_1).code(200).message("OK").build()
        }
        MapPrivacyInterceptor(config).intercept(chain)
        assertEquals("GET",clean.captured.method);assertNull(clean.captured.body)
        assertEquals(setOf("User-Agent","Accept"),clean.captured.headers.names())
        assertEquals(original.url,clean.captured.url)
    }
    @Test fun externalStylesTelemetryAndRedirectTargetsAreDeniedBeforeNetwork() {
        val chain=mockk<Interceptor.Chain>()
        every{chain.request()} returns Request.Builder().url("https://telemetry.example.test/events").build()
        var failed=false
        try {MapPrivacyInterceptor(config){failed=true}.intercept(chain);fail("Must fail closed")}
        catch(_:IOException){}
        assertTrue(failed);verify(exactly=0){chain.proceed(any())}
    }
    @Test fun networkAndHttpFailureArePresentationSignalsOnly() {
        val chain=mockk<Interceptor.Chain>()
        val request=Request.Builder().url("https://tiles.example.test/1/2/3.png").build()
        every{chain.request()} returns request
        every{chain.proceed(any())} returns Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(503).message("Unavailable").build()
        var failures=0
        assertEquals(503,MapPrivacyInterceptor(config){failures++}.intercept(chain).code)
        every{chain.proceed(any())} throws IOException("offline")
        try {MapPrivacyInterceptor(config){failures++}.intercept(chain);fail("Expected network failure")}
        catch(_:IOException){}
        assertEquals(2,failures)
    }
}
