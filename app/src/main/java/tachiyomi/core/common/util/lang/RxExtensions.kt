package tachiyomi.core.common.util.lang

import kotlinx.coroutines.suspendCancellableCoroutine
import rx.Observable
import rx.Subscriber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Observable<T>.awaitSingle(): T = suspendCancellableCoroutine { cont ->
    val sub = subscribe(object : Subscriber<T>() {
        private var value: T? = null
        private var hasValue = false

        override fun onStart() {
            request(1)
        }

        override fun onNext(t: T) {
            value = t
            hasValue = true
        }

        override fun onCompleted() {
            if (cont.isActive) {
                if (hasValue) {
                    @Suppress("UNCHECKED_CAST")
                    cont.resume(value as T)
                } else {
                    cont.resumeWithException(NoSuchElementException("Observable completed without emitting any elements"))
                }
            }
        }

        override fun onError(e: Throwable) {
            if (cont.isActive) {
                cont.resumeWithException(e)
            }
        }
    })
    cont.invokeOnCancellation {
        sub.unsubscribe()
    }
}
