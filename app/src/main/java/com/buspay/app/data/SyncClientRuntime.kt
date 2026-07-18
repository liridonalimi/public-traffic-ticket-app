package com.buspay.app.data

enum class SyncRuntimeMode {
    DEMO,
    LOCAL_VALIDATION,
    PRODUCTION
}

data class SyncRuntimeConfig(
    val mode: SyncRuntimeMode,
    val endpointUrl: String? = null,
    val accessToken: String? = null
) {
    companion object {
        fun demo(): SyncRuntimeConfig = SyncRuntimeConfig(mode = SyncRuntimeMode.DEMO)

        fun localValidation(endpointUrl: String, accessToken: String): SyncRuntimeConfig =
            SyncRuntimeConfig(
                mode = SyncRuntimeMode.LOCAL_VALIDATION,
                endpointUrl = endpointUrl,
                accessToken = accessToken
            )

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
    SyncRuntimeMode.LOCAL_VALIDATION -> ProductionTransitSyncClient(
        ProductionSyncConfig.localValidation(
            endpointUrl = requireNotNull(config.endpointUrl) {
                "Local validation sync endpoint is required"
            },
            accessToken = requireNotNull(config.accessToken) {
                "Local validation access token is required"
            }
        )
    )
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

fun createManagedCatalogClient(config: SyncRuntimeConfig): ManagedCatalogClient? = when (config.mode) {
    SyncRuntimeMode.DEMO -> null
    SyncRuntimeMode.LOCAL_VALIDATION -> ManagedCatalogClient(
        ProductionSyncConfig.localValidation(
            endpointUrl = requireNotNull(config.endpointUrl),
            accessToken = requireNotNull(config.accessToken)
        )
    )
    SyncRuntimeMode.PRODUCTION -> ManagedCatalogClient(
        ProductionSyncConfig(
            endpointUrl = requireNotNull(config.endpointUrl),
            accessToken = requireNotNull(config.accessToken)
        )
    )
}
