package com.buspay.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

class SyncClientRuntimeTest {
    @Test
    fun `demo runtime creates demo client`() {
        assertTrue(createTransitSyncClient(SyncRuntimeConfig.demo()) is DemoTransitSyncClient)
    }

    @Test
    fun `production runtime creates HTTPS client`() {
        val client = createTransitSyncClient(
            SyncRuntimeConfig.production(
                endpointUrl = "https://sync.buspay.example/v1/sync",
                accessToken = "short-lived-token"
            )
        )

        assertTrue(client is ProductionTransitSyncClient)
    }

    @Test
    fun `local validation runtime creates authenticated client for loopback`() {
        val client = createTransitSyncClient(
            SyncRuntimeConfig.localValidation(
                endpointUrl = "http://127.0.0.1:8080/v1/sync",
                accessToken = "local-token"
            )
        )

        assertTrue(client is ProductionTransitSyncClient)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `local validation runtime rejects non loopback cleartext endpoint`() {
        createTransitSyncClient(
            SyncRuntimeConfig.localValidation(
                endpointUrl = "http://192.168.1.10:8080/v1/sync",
                accessToken = "local-token"
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `production runtime rejects cleartext endpoint`() {
        createTransitSyncClient(
            SyncRuntimeConfig.production(
                endpointUrl = "http://sync.buspay.example/v1/sync",
                accessToken = "short-lived-token"
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `production runtime requires authenticated session values`() {
        createTransitSyncClient(SyncRuntimeConfig(mode = SyncRuntimeMode.PRODUCTION))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `local validation runtime requires authenticated session values`() {
        createTransitSyncClient(SyncRuntimeConfig(mode = SyncRuntimeMode.LOCAL_VALIDATION))
    }
}
