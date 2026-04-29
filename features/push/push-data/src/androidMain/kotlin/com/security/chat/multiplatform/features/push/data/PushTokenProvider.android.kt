package com.security.chat.multiplatform.features.push.data

import com.google.firebase.messaging.FirebaseMessaging
import com.security.chat.multiplatform.common.log.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual suspend fun getToken(): String? {
    return suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                continuation.resume(token)
            }
            .addOnFailureListener { error ->
                Log.e(error = error, message = "Failed to fetch FCM token")
                continuation.resumeWithException(error)
            }
            .addOnCanceledListener {
                continuation.resume(null)
            }
    }
}