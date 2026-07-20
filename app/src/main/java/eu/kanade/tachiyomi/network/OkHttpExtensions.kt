package eu.kanade.tachiyomi.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import rx.Subscription
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resumeWithException

fun Call.asObservable(): Observable<Response> {
    return Observable.create(Observable.OnSubscribe<Response> { subscriber ->
        val cancelled = AtomicBoolean()
        subscriber.add(object : Subscription {
            override fun unsubscribe() {
                if (cancelled.compareAndSet(false, true)) {
                    cancel()
                }
            }
            override fun isUnsubscribed(): Boolean = cancelled.get() || isCanceled()
        })
        try {
            if (!subscriber.isUnsubscribed) {
                val response = execute()
                subscriber.onNext(response)
                subscriber.onCompleted()
            }
        } catch (e: Throwable) {
            if (!subscriber.isUnsubscribed) {
                subscriber.onError(e)
            }
        }
    })
}

fun Call.asObservableSuccess(): Observable<Response> {
    return asObservable().map { response ->
        if (!response.isSuccessful) {
            response.close()
            throw HttpException(response.code)
        }
        response
    }
}

private suspend fun Call.await(callStack: Array<StackTraceElement>): Response {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            try {
                this.cancel()
            } catch (_: Throwable) {}
        }

        this.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                val exception = IOException(e.message, e).apply { stackTrace = callStack }
                continuation.resumeWithException(exception)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, value, _ ->
                    value.close()
                }
            }
        })
    }
}

suspend fun Call.await(): Response {
    val callStack = Exception().stackTrace.run { copyOfRange(1, size) }
    return await(callStack)
}

suspend fun Call.awaitSuccess(): Response {
    val callStack = Exception().stackTrace.run { copyOfRange(1, size) }
    val response = await(callStack)
    if (!response.isSuccessful) {
        response.close()
        throw HttpException(response.code).apply { stackTrace = callStack }
    }
    return response
}

fun OkHttpClient.newCachelessCallWithProgress(
    request: Request,
    listener: ProgressListener,
    existingSize: Long = 0L,
): Call {
    val progressClient = newBuilder()
        .cache(null)
        .addNetworkInterceptor { chain ->
            // Capture original request first so it can be referenced inside apply {}
            val originalRequest = chain.request()
            val req = originalRequest
                .newBuilder()
                .apply {
                    if (existingSize > 0 && originalRequest.header("Range") == null) {
                        header("Range", "bytes=$existingSize-")
                    }
                }
                .build()

            val originalResponse = chain.proceed(req)
            // Only wrap if body is present — Response.Builder.body() requires non-null ResponseBody
            val originalBody = originalResponse.body
            if (originalBody != null) {
                originalResponse.newBuilder()
                    .body(ProgressResponseBody(originalBody, listener, existingSize))
                    .build()
            } else {
                originalResponse
            }
        }
        .build()

    return progressClient.newCall(request)
}
