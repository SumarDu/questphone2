package launcher.launcher.data

enum class IntegrationId {
    /**
     * Block all apps except a specific selected app.
     * Used in scenarios wherein user wants to focus on a specific app lets say a language learning app without any distraction.
     * this quest can be used in such scenarios
     */
    APP_FOCUS,

    /**
     * blocks all apps except a few selected ones.
     * Used in scenarios wherein user wants to block everything except a few necessary apps like phone, messaging, gmail, music etc.
     * Useful when user wants to block access to his phone and focus on some irl task like studying
     */
    DEEP_FOCUS,


    HEALTH,

    HEALTH_API_RUNNING,
    HEALTH_API_WORKOUT,
    HEALTH_API_WATER_INTAKE
}