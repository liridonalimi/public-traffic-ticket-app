package com.buspay.app.data

enum class SyncRuntimeMode {
    DEMO,
    PRODUCTION
}

data class SyncRuntimeConfig(
    val mode: SyncRuntimeMode,
    val endpointUrl: String? = null,
    val accessToken: String? = null
) {
    companion object {
        fun demo(): SyncRuntimeConfig = SyncRuntimeConfig(mode = SyncRuntimeMode.DEMO)

        fun production(endpointUrl: String, accessToken: String): SyncRuntimeConfig =
            SyncRuntimeConfig(
                mode = SyncRuntimeMode.PRODUCTION,
                endpointUrl = endpointUrl,
                accessToken = accessToken
            )
    }
}

fun createTransitSyncClient(config: SyncRuntimeConfig): TransitSyncClient = when (config.mode) {
    SyncRuntimeMode.DEMO -> DemoTransitSyncClient()
    SyncRuntimeMode.PRODUCTION -> ProductionTransitSyncClient(
        ProductionSyncConfig(
            endpointUrl = requireNotNull(config.endpointUrl) {
                "Production sync endpoint is required"
            },
            accessToken = requireNotNull(config.accessToken) {
                "Production sync access token is required"
            }
        )
    )
}
