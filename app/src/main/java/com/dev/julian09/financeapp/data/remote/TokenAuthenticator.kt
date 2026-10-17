package com.dev.julian09.financeapp.data.remote

import com.dev.julian09.financeapp.data.remote.dto.RefreshRequestDto
import com.dev.julian09.financeapp.data.session.DeviceIdProvider
import com.dev.julian09.financeapp.data.session.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * On a 401 renews the access token once and retries (SEQ-03). Simultaneous 401s share one refresh: whoever
 * gets the lock second sees a newer token and just retries. A rejected refresh marks the session EXPIRED.
 */
@Singleton
class TokenAuthenticator
    @Inject
    constructor(
        private val store: SessionStore,
        private val deviceId: DeviceIdProvider,
        // Provider: AuthApi is built on the OkHttp client that holds this authenticator
        private val api: Provider<AuthApi>,
    ) : Authenticator {
        private val lock = Any()

        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            val failed = response.request
            val sentToken = failed.header(AuthInterceptor.AUTHORIZATION)?.removePrefix("Bearer ") ?: return null
            if (failed.url.encodedPath in AuthApi.PUBLIC_PATHS || response.priorResponse != null) return null

            synchronized(lock) {
                val session = runBlocking { store.current() }?.takeUnless { it.expired } ?: return null
                if (session.accessToken != sentToken) return failed.withToken(session.accessToken)

                val refreshed =
                    try {
                        api.get().refresh(RefreshRequestDto(session.refreshToken, deviceId.deviceId)).execute()
                    } catch (e: IOException) {
                        // No network is not a lost session: the caller just fails and retries later
                        return null
                    }
                val body = refreshed.body()
                if (!refreshed.isSuccessful || body == null) {
                    if (refreshed.code() == 401) runBlocking { store.markExpired() }
                    return null
                }
                runBlocking {
                    store.save(session.copy(accessToken = body.accessToken, refreshToken = body.refreshToken, user = body.user.toStored()))
                }
                return failed.withToken(body.accessToken)
            }
        }

        private fun Request.withToken(token: String) = newBuilder().header(AuthInterceptor.AUTHORIZATION, "Bearer $token").build()
    }
