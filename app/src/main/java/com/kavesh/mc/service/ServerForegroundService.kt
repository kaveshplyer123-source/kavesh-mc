package com.kavesh.mc.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.kavesh.mc.MainActivity
import com.kavesh.mc.R
import com.kavesh.mc.model.LogLevel
import com.kavesh.mc.model.ServerConfig
import com.kavesh.mc.model.ServerStatus
import com.kavesh.mc.model.TunnelMode
import com.kavesh.mc.util.ConfigWriter
import com.kavesh.mc.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ServerForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "kavesh_mc_server_channel"
        const val NOTIF_ID = 1001
        const val ACTION_START = "com.kavesh.mc.ACTION_START"
        const val ACTION_STOP = "com.kavesh.mc.ACTION_STOP"
        const val EXTRA_SERVER_ID = "server_id"
        const val EXTRA_SERVER_NAME = "server_name"
        const val EXTRA_PORT = "port"

        var pendingConfig: ServerConfig? = null
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverProcess: Process? = null
    private var tunnelProcess: Process? = null
    private var stdinWriter: BufferedWriter? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopServerProcess()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val config = pendingConfig ?: ServerConfig(
                    id = intent.getStringExtra(EXTRA_SERVER_ID) ?: "default",
                    name = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Smp Server",
                    port = intent.getIntExtra(EXTRA_PORT, 25565)
                )
                startServerProcess(config)
            }
        }
        return START_STICKY
    }

    private fun startServerProcess(config: ServerConfig) {
        acquireWakeLock()

        val localIp = "${NetworkUtils.getLocalIpAddress()}:${config.port}"
        ServerRuntimeState.clearLogs()
        ServerRuntimeState.updateState(
            status = ServerStatus.STARTING,
            serverId = config.id,
            localIp = localIp,
            publicIp = null
        )

        startForeground(
            NOTIF_ID,
            buildNotification("Starting ${config.name}...", "LAN IP: $localIp")
        )

        ServerRuntimeState.appendLog(LogLevel.SYSTEM, "Starting ${config.name} (${config.software} ${config.version})...")
        ServerRuntimeState.appendLog(LogLevel.INFO, "Local LAN Address ready: $localIp")

        val serverDir = File(filesDir, "servers/${config.id}")
        ConfigWriter.writeServerFiles(serverDir, config)
        ServerRuntimeState.appendLog(LogLevel.INFO, "Wrote configuration.toml & server.properties to ${serverDir.name}/")

        serviceScope.launch {
            val nativeDir = File(applicationInfo.nativeLibraryDir)
            val pumpkinBin = File(nativeDir, "libpumpkin.so")

            if (!pumpkinBin.exists()) {
                ServerRuntimeState.appendLog(
                    LogLevel.WARN,
                    "Native binary libpumpkin.so not bundled yet in jniLibs/arm64-v8a/."
                )
                ServerRuntimeState.appendLog(
                    LogLevel.SYSTEM,
                    "Place your ARM64 PumpkinMC binary as app/src/main/jniLibs/arm64-v8a/libpumpkin.so and rebuild APK."
                )
                ServerRuntimeState.appendLog(
                    LogLevel.INFO,
                    "Server address when binary is bundled: $localIp"
                )
                ServerRuntimeState.updateState(
                    status = ServerStatus.OFFLINE,
                    serverId = config.id,
                    localIp = localIp,
                    publicIp = null
                )
                stopSelf()
                return@launch
            }

            try {
                val mode = ServerRuntimeState.tunnelMode.value
                if (mode != TunnelMode.LOCAL) {
                    startTunnelProcess(nativeDir, mode, config.port)
                }

                val pb = ProcessBuilder(pumpkinBin.absolutePath)
                    .directory(serverDir)
                    .redirectErrorStream(true)

                val proc = pb.start()
                serverProcess = proc
                stdinWriter = BufferedWriter(OutputStreamWriter(proc.outputStream))

                ServerRuntimeState.commandSender = { cmd ->
                    serviceScope.launch {
                        try {
                            stdinWriter?.write(cmd)
                            stdinWriter?.newLine()
                            stdinWriter?.flush()
                            ServerRuntimeState.appendLog(LogLevel.SYSTEM, "> $cmd")
                        } catch (e: Exception) {
                            ServerRuntimeState.appendLog(LogLevel.ERROR, "Failed to send command: ${e.message}")
                        }
                    }
                }

                ServerRuntimeState.updateState(
                    status = ServerStatus.ONLINE,
                    serverId = config.id,
                    localIp = localIp
                )
                updateNotification("${config.name} is ONLINE", "Join at $localIp")
                ServerRuntimeState.appendLog(LogLevel.INFO, "Server listening on $localIp (0.0.0.0:${config.port})")

                val ansiRegex = Regex("""\u001B\[[;\d]*m""")
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val clean = line!!.replace(ansiRegex, "")
                    val level = when {
                        clean.contains("WARN", ignoreCase = true) -> LogLevel.WARN
                        clean.contains("ERROR", ignoreCase = true) -> LogLevel.ERROR
                        else -> LogLevel.INFO
                    }
                    ServerRuntimeState.appendLog(level, clean)
                }

                val exitCode = proc.waitFor()
                ServerRuntimeState.appendLog(LogLevel.SYSTEM, "pumpkin_server exited with code $exitCode")
            } catch (e: Exception) {
                ServerRuntimeState.appendLog(LogLevel.ERROR, "Process launch error: ${e.message}")
            } finally {
                cleanupAndStop()
            }
        }
    }

    private fun startTunnelProcess(nativeDir: File, mode: TunnelMode, port: Int) {
        val binName = if (mode == TunnelMode.PLAYIT) "libplayit.so" else "libbore.so"
        val bin = File(nativeDir, binName)
        if (!bin.exists()) {
            ServerRuntimeState.appendLog(
                LogLevel.WARN,
                "Tunnel binary $binName not found in jniLibs/arm64-v8a/. Continuing in Local LAN mode."
            )
            return
        }

        val cmd = if (mode == TunnelMode.PLAYIT) {
            listOf(bin.absolutePath)
        } else {
            listOf(bin.absolutePath, "local", port.toString(), "--to", "bore.pub")
        }

        serviceScope.launch {
            try {
                ServerRuntimeState.appendLog(LogLevel.SYSTEM, "Starting ${mode.label} tunnel...")
                val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
                tunnelProcess = proc
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                var line: String?
                val boreRegex = Regex("""bore\.pub:\d+""")
                val playitRegex = Regex("""[a-zA-Z0-9-]+\.gl\.joinmc\.link(:\d+)?""")

                while (reader.readLine().also { line = it } != null) {
                    val text = line!!
                    ServerRuntimeState.appendLog(LogLevel.SYSTEM, "[TUNNEL] $text")
                    val foundPublic = boreRegex.find(text)?.value ?: playitRegex.find(text)?.value
                    if (foundPublic != null) {
                        ServerRuntimeState.updateState(
                            status = ServerRuntimeState.status.value,
                            publicIp = foundPublic
                        )
                        updateNotification("Kavesh-MC Online", "Public: $foundPublic")
                    }
                }
            } catch (e: Exception) {
                ServerRuntimeState.appendLog(LogLevel.ERROR, "Tunnel error: ${e.message}")
            }
        }
    }

    private fun stopServerProcess() {
        ServerRuntimeState.updateState(status = ServerStatus.STOPPING)
        ServerRuntimeState.appendLog(LogLevel.SYSTEM, "Stopping server...")
        serviceScope.launch {
            try {
                stdinWriter?.write("stop")
                stdinWriter?.newLine()
                stdinWriter?.flush()
                Thread.sleep(1500)
            } catch (_: Exception) {}
            serverProcess?.destroyForcibly()
            tunnelProcess?.destroyForcibly()
            cleanupAndStop()
        }
    }

    private fun cleanupAndStop() {
        ServerRuntimeState.commandSender = null
        ServerRuntimeState.updateState(
            status = ServerStatus.OFFLINE,
            publicIp = null
        )
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KaveshMC::ServerWakeLock")
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(12 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kavesh-MC Server Runtime",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_kavesh)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(title, text))
    }

    override fun onDestroy() {
        serverProcess?.destroyForcibly()
        tunnelProcess?.destroyForcibly()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
