package com.kavesh.mc.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.kavesh.mc.model.GameplaySettings
import com.kavesh.mc.model.PerformanceSettings
import com.kavesh.mc.model.ServerConfig
import com.kavesh.mc.model.TunnelMode
import com.kavesh.mc.service.ServerForegroundService
import com.kavesh.mc.service.ServerRuntimeState
import com.kavesh.mc.util.ConfigWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class KaveshViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("kavesh_mc_prefs", Context.MODE_PRIVATE)
    private val serversRoot = File(application.filesDir, "servers").apply { mkdirs() }

    private val _servers = MutableStateFlow<List<ServerConfig>>(emptyList())
    val servers: StateFlow<List<ServerConfig>> = _servers.asStateFlow()

    val status = ServerRuntimeState.status
    val activeServerId = ServerRuntimeState.activeServerId
    val localAddress = ServerRuntimeState.localAddress
    val publicAddress = ServerRuntimeState.publicAddress
    val tunnelMode = ServerRuntimeState.tunnelMode
    val logs = ServerRuntimeState.logs

    init {
        loadServers()
    }

    fun getServersRootDir(): File = serversRoot

    fun setTunnelMode(mode: TunnelMode) {
        ServerRuntimeState.setTunnelMode(mode)
    }

    fun startServer(server: ServerConfig) {
        val ctx = getApplication<Application>()
        ServerForegroundService.pendingConfig = server
        val intent = Intent(ctx, ServerForegroundService::class.java).apply {
            action = ServerForegroundService.ACTION_START
            putExtra(ServerForegroundService.EXTRA_SERVER_ID, server.id)
            putExtra(ServerForegroundService.EXTRA_SERVER_NAME, server.name)
            putExtra(ServerForegroundService.EXTRA_PORT, server.port)
        }
        ContextCompat.startForegroundService(ctx, intent)
    }

    fun stopServer() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, ServerForegroundService::class.java).apply {
            action = ServerForegroundService.ACTION_STOP
        }
        ctx.startService(intent)
    }

    fun sendCommand(raw: String) {
        val clean = raw.trim().removePrefix("/")
        if (clean.isNotEmpty()) {
            ServerRuntimeState.commandSender?.invoke(clean)
        }
    }

    fun addOrUpdateServer(config: ServerConfig) {
        val list = _servers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == config.id }
        if (idx >= 0) list[idx] = config else list.add(0, config)
        _servers.value = list
        saveServers()
        ConfigWriter.writeServerFiles(File(serversRoot, config.id), config)
    }

    fun deleteServer(serverId: String) {
        if (activeServerId.value == serverId) stopServer()
        _servers.value = _servers.value.filterNot { it.id == serverId }
        saveServers()
        File(serversRoot, serverId).deleteRecursively()
    }

    private fun loadServers() {
        val raw = prefs.getString("servers_json", null)
        if (raw.isNullOrBlank()) {
            val defaultServer = ServerConfig(
                id = "smp_server_default",
                name = "Smp Server",
                software = "Pumpkin",
                version = "26.3"
            )
            _servers.value = listOf(defaultServer)
            saveServers()
            ConfigWriter.writeServerFiles(File(serversRoot, defaultServer.id), defaultServer)
            return
        }
        try {
            val arr = JSONArray(raw)
            val parsed = mutableListOf<ServerConfig>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val g = o.optJSONObject("gameplay") ?: JSONObject()
                val p = o.optJSONObject("performance") ?: JSONObject()
                parsed.add(
                    ServerConfig(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        software = o.optString("software", "Pumpkin"),
                        version = o.optString("version", "26.3"),
                        port = o.optInt("port", 25565),
                        onlineMode = o.optBoolean("onlineMode", false),
                        motd = o.optString("motd", "Powered with ❤️ by Kavesh-MC"),
                        worldType = o.optString("worldType", "Normal"),
                        seed = o.optString("seed", ""),
                        eulaAccepted = o.optBoolean("eulaAccepted", true),
                        gameplay = GameplaySettings(
                            gamemode = g.optString("gamemode", "Survival"),
                            difficulty = g.optString("difficulty", "Normal"),
                            hardcore = g.optBoolean("hardcore", false),
                            preset = g.optString("preset", "Default"),
                            keepInventory = g.optBoolean("keepInventory", false),
                            mobGriefing = g.optBoolean("mobGriefing", true),
                            commandBlocks = g.optBoolean("commandBlocks", false),
                            spawnProtection = g.optInt("spawnProtection", 16),
                            kickIdleMinutes = g.optString("kickIdleMinutes", "Off"),
                            resourcePackUrl = g.optString("resourcePackUrl", "")
                        ),
                        performance = PerformanceSettings(
                            allocatedRamGb = p.optDouble("allocatedRamGb", 3.5).toFloat(),
                            performanceMode = p.optBoolean("performanceMode", false),
                            viewDistance = p.optInt("viewDistance", 6),
                            simulationDistance = p.optInt("simulationDistance", 5),
                            optimizeForPhone = p.optBoolean("optimizeForPhone", true),
                            maxPlayers = p.optInt("maxPlayers", 5)
                        )
                    )
                )
            }
            _servers.value = parsed
        } catch (_: Exception) {
            _servers.value = listOf(ServerConfig())
        }
    }

    private fun saveServers() {
        val arr = JSONArray()
        for (s in _servers.value) {
            val g = JSONObject().apply {
                put("gamemode", s.gameplay.gamemode)
                put("difficulty", s.gameplay.difficulty)
                put("hardcore", s.gameplay.hardcore)
                put("preset", s.gameplay.preset)
                put("keepInventory", s.gameplay.keepInventory)
                put("mobGriefing", s.gameplay.mobGriefing)
                put("commandBlocks", s.gameplay.commandBlocks)
                put("spawnProtection", s.gameplay.spawnProtection)
                put("kickIdleMinutes", s.gameplay.kickIdleMinutes)
                put("resourcePackUrl", s.gameplay.resourcePackUrl)
            }
            val p = JSONObject().apply {
                put("allocatedRamGb", s.performance.allocatedRamGb.toDouble())
                put("performanceMode", s.performance.performanceMode)
                put("viewDistance", s.performance.viewDistance)
                put("simulationDistance", s.performance.simulationDistance)
                put("optimizeForPhone", s.performance.optimizeForPhone)
                put("maxPlayers", s.performance.maxPlayers)
            }
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("software", s.software)
                put("version", s.version)
                put("port", s.port)
                put("onlineMode", s.onlineMode)
                put("motd", s.motd)
                put("worldType", s.worldType)
                put("seed", s.seed)
                put("eulaAccepted", s.eulaAccepted)
                put("gameplay", g)
                put("performance", p)
            })
        }
        prefs.edit().putString("servers_json", arr.toString()).apply()
    }
}
