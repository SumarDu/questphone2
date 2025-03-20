package launcher.launcher.data

enum class IntegrationId {
    /**
     * blocks all apps except a few selected ones.
     * Used in scenarios wherein user wants to block everything except a few necessary apps like phone, messaging, gmail, music etc.
     * Useful when user wants to block access to his phone and focus on some irl task like studying
     */
    DEEP_FOCUS,


    HEALTH_CONNECT,

    SWIFT_MARK,

    AI_SNAP
}