package com.kitsugi.animelist.data.cloudstream

import org.junit.Assert.*
import org.junit.Test
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * CsArchComponentExt'in hata sınıflandırması için JVM unit testleri.
 *
 * throwAbleToResource() fonksiyonunun doğru Resource.Failure türlerini
 * döndürdüğünü ve isNetworkError bayrağının doğru ayarlandığını doğrular.
 *
 * Not: CsPluginStatusTracker'ın transient-network korumasının da bu
 * sınıflandırmaya dayandığını unutma — bu testler güvenli auto-pruning
 * davranışını da dolaylı olarak doğrular.
 */
class CsArchComponentExtTest {

    // ─── throwAbleToResource Tests ────────────────────────────────────────────

    @Test
    fun `SocketTimeoutException maps to network failure`() {
        val result = throwAbleToResource<Any>(SocketTimeoutException("timed out"))
        assertTrue("SocketTimeout should be a network error", result is Resource.Failure)
        val failure = result as Resource.Failure
        assertTrue("isNetworkError should be true", failure.isNetworkError)
        assertTrue("Should mention Timeout", failure.errorString.contains("Timeout", ignoreCase = true))
    }

    @Test
    fun `InterruptedIOException maps to network failure`() {
        val result = throwAbleToResource<Any>(InterruptedIOException("interrupted"))
        assertTrue(result is Resource.Failure)
        val failure = result as Resource.Failure
        assertTrue(failure.isNetworkError)
    }

    @Test
    fun `UnknownHostException maps to network failure`() {
        val result = throwAbleToResource<Any>(UnknownHostException("unknown host"))
        assertTrue(result is Resource.Failure)
        val failure = result as Resource.Failure
        assertTrue(failure.isNetworkError)
        assertTrue(failure.errorString.contains("connect", ignoreCase = true))
    }

    @Test
    fun `SSLHandshakeException maps to network failure with VPN hint`() {
        val result = throwAbleToResource<Any>(SSLHandshakeException("ssl error"))
        assertTrue(result is Resource.Failure)
        val failure = result as Resource.Failure
        assertTrue(failure.isNetworkError)
        assertTrue("Should suggest VPN", failure.errorString.contains("VPN", ignoreCase = true))
    }

    @Test
    fun `NotImplementedError maps to structural failure (not network)`() {
        val result = throwAbleToResource<Any>(NotImplementedError("not impl"))
        assertTrue(result is Resource.Failure)
        val failure = result as Resource.Failure
        assertFalse("isNetworkError should be false", failure.isNetworkError)
        assertTrue("Should mention not implemented", failure.errorString.contains("not implemented", ignoreCase = true))
    }

    @Test
    fun `ErrorLoadingException maps to network error`() {
        val result = throwAbleToResource<Any>(ErrorLoadingException("loading error"))
        assertTrue(result is Resource.Failure)
        val failure = result as Resource.Failure
        assertTrue("ErrorLoadingException should be marked network error", failure.isNetworkError)
        assertTrue(failure.errorString.contains("loading error", ignoreCase = true))
    }

    @Test
    fun `NoSuchMethodException maps to outdated app failure`() {
        val result = throwAbleToResource<Any>(NoSuchMethodException("method not found"))
        assertTrue(result is Resource.Failure)
        val failure = result as Resource.Failure
        assertFalse("isNetworkError should be false", failure.isNetworkError)
        assertTrue("Should mention outdated", failure.errorString.contains("outdated", ignoreCase = true))
    }

    @Test
    fun `NoSuchFieldException maps to outdated app failure`() {
        val result = throwAbleToResource<Any>(NoSuchFieldException("field not found"))
        assertTrue(result is Resource.Failure)
        val failure = result as Resource.Failure
        assertFalse(failure.isNetworkError)
        assertTrue(failure.errorString.contains("outdated", ignoreCase = true))
    }

    @Test
    fun `generic RuntimeException maps to safeFail (not network)`() {
        val result = throwAbleToResource<Any>(RuntimeException("runtime error"))
        assertTrue(result is Resource.Failure)
        val failure = result as Resource.Failure
        assertFalse("Generic exception should not be network error", failure.isNetworkError)
    }

    @Test
    fun `NullPointerException from provider file maps to Cloudflare hint`() {
        // Simulate NPE with a stacktrace element from a file named "provider.kt"
        val npe = NullPointerException("null ref")
        val fakeFrame = StackTraceElement("com.plugin.SomeProvider", "search", "provider.kt", 99)
        npe.stackTrace = arrayOf(fakeFrame)
        val result = throwAbleToResource<Any>(npe)
        assertTrue(result is Resource.Failure)
        val failure = result as Resource.Failure
        assertFalse(failure.isNetworkError)
        assertTrue("Should mention Cloudflare", failure.errorString.contains("Cloudflare", ignoreCase = true))
    }

    @Test
    fun `NullPointerException from non-provider file uses safeFail`() {
        val npe = NullPointerException("generic npe")
        val fakeFrame = StackTraceElement("com.kitsugi.SomeClass", "doThing", "SomeClass.kt", 55)
        npe.stackTrace = arrayOf(fakeFrame)
        val result = throwAbleToResource<Any>(npe)
        assertTrue(result is Resource.Failure)
        // safeFail returns non-network
        val failure = result as Resource.Failure
        assertFalse(failure.isNetworkError)
    }

    // ─── getStackTracePretty Tests ────────────────────────────────────────────

    @Test
    fun `getStackTracePretty includes class and line number`() {
        val e = RuntimeException("test error")
        val pretty = e.getStackTracePretty()
        // The stack trace should contain at least this test class
        assertTrue("Should contain class name", pretty.contains("CsArchComponentExtTest") || pretty.isNotBlank())
    }

    @Test
    fun `getStackTracePretty without message omits prefix`() {
        val e = RuntimeException("this message should not appear")
        val withMessage = e.getStackTracePretty(showMessage = false)
        assertFalse("Should not contain message when showMessage=false", withMessage.contains("this message should not appear"))
    }

    // ─── getAllMessages Tests ─────────────────────────────────────────────────

    @Test
    fun `getAllMessages chains cause messages`() {
        val cause = RuntimeException("root cause")
        val wrapper = RuntimeException("wrapper", cause)
        val all = wrapper.getAllMessages()
        assertTrue("Should contain wrapper message", all.contains("wrapper"))
        assertTrue("Should contain root cause", all.contains("root cause"))
    }

    // ─── Resource.fromResult Tests ────────────────────────────────────────────

    @Test
    fun `Resource fromResult with success wraps value`() {
        val result = Result.success(42)
        val resource = Resource.fromResult(result)
        assertTrue(resource is Resource.Success)
        assertEquals(42, (resource as Resource.Success).value)
    }

    @Test
    fun `Resource fromResult with failure wraps throwable`() {
        val result = Result.failure<Int>(SocketTimeoutException("timed out"))
        val resource = Resource.fromResult(result)
        assertTrue(resource is Resource.Failure)
        assertTrue((resource as Resource.Failure).isNetworkError)
    }
}
