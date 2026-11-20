package com.kavesh.mc.model

import java.util.UUID

enum class ServerStatus {
    OFFLINE, STARTING, ONLINE, STOPPING
}

enum class TunnelMode(val label: String, val badge: String) {
    LOCAL("Local LAN", "Local"),
    PLAYIT("Playit.gg Tunnel", "Playit"),
    BORE("Bore.pub Tunnel", "Bore")
}

enum class LogLevel {
    INFO, WARN, ERROR, SYSTEM
}

data class ConsoleLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val level: LogLevel,
    val message: String
)

data class GameplaySettings(
    val gamemode: String = "Survival",
    val difficulty: String = "Normal",
    val hardcore: Boolean = false,
    val preset: String = "Default",
    val keepInventory: Boolean = false,
    val mobGriefing: Boolean = true,
    val commandBlocks: Boolean = false,
    val spawnProtection: Int = 16,
    val kickIdleMinutes: String = "Off",
    val resourcePackUrl: String = ""
)

data class PerformanceSettings(
    val allocatedRamGb: Float = 3.5f,
    val performanceMode: Boolean = false,
    val viewDistance: Int = 6,
    val simulationDistance: Int = 5,
    val optimizeForPhone: Boolean = true,
    val maxPlayers: Int = 5
)

data class ServerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Smp Server",
    val software: String = "Pumpkin",
    val version: String = "26.3",
    val port: Int = 25565,
    val onlineMode: Boolean = false,
    val motd: String = "Powered with ❤️ by Kavesh-MC",
    val worldType: String = "Normal",
    val seed: String = "",
    val eulaAccepted: Boolean = true,
    val gameplay: GameplaySettings = GameplaySettings(),
    val performance: PerformanceSettings = PerformanceSettings()
)

data class ModrinthProject(
    val projectId: String,
    val slug: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val downloads: Int
)
