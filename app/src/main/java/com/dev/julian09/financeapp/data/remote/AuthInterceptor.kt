package com.dev.julian09.financeapp.data.remote

import com.dev.julian09.financeapp.data.session.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Adds "Authorization: Bearer" to every request except the public auth routes. Runs on OkHttp's threads. */
class AuthInterceptor
    @Inject
    constructor(
        private val store: SessionStore,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (request.url.encodedPath in AuthApi.PUBLIC_PATHS) return chain.proceed(request)
            // DataStore keeps the value in memory after the first read; this is not a disk read per request
            val token = runBlocking { store.current() }?.takeUnless { it.expired }?.accessToken
            return chain.proceed(if (token == null) request else request.newBuilder().header(AUTHORIZATION, "Bearer $token").build())
        }

        companion object {
            const val AUTHORIZATION = "Authorization"
        }
    }
