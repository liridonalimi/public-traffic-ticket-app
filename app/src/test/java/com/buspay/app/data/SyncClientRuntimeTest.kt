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
}
