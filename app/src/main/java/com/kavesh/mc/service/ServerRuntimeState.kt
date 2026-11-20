package com.kavesh.mc.service

import com.kavesh.mc.model.ConsoleLog
import com.kavesh.mc.model.LogLevel
import com.kavesh.mc.model.ServerStatus
import com.kavesh.mc.model.TunnelMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ServerRuntimeState {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _status = MutableStateFlow(ServerStatus.OFFLINE)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    private val _activeServerId = MutableStateFlow<String?>(null)
    val activeServerId: StateFlow<String?> = _activeServerId.asStateFlow()

    private val _localAddress = MutableStateFlow<String?>(null)
    val localAddress: StateFlow<String?> = _localAddress.asStateFlow()

    private val _publicAddress = MutableStateFlow<String?>(null)
    val publicAddress: StateFlow<String?> = _publicAddress.asStateFlow()

    private val _tunnelMode = MutableStateFlow(TunnelMode.LOCAL)
    val tunnelMode: StateFlow<TunnelMode> = _tunnelMode.asStateFlow()

    private val _logs = MutableStateFlow<List<ConsoleLog>>(emptyList())
    val logs: StateFlow<List<ConsoleLog>> = _logs.asStateFlow()

    var commandSender: ((String) -> Unit)? = null

    fun setTunnelMode(mode: TunnelMode) {
        _tunnelMode.value = mode
    }

    fun updateState(
        status: ServerStatus,
        serverId: String? = _activeServerId.value,
        localIp: String? = _localAddress.value,
        publicIp: String? = _publicAddress.value
    ) {
        _status.value = status
        _activeServerId.value = serverId
        _localAddress.value = localIp
        _publicAddress.value = publicIp
    }

    fun appendLog(level: LogLevel, message: String) {
        val entry = ConsoleLog(
            timestamp = timeFormat.format(Date()),
            level = level,
            message = message
        )
        val current = _logs.value
        _logs.value = (current + entry).takeLast(500)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
