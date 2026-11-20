package com.kavesh.mc.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavesh.mc.model.ServerConfig
import com.kavesh.mc.model.ServerStatus
import com.kavesh.mc.model.TunnelMode
import com.kavesh.mc.ui.KaveshViewModel
import com.kavesh.mc.ui.components.KaveshBrandSquare
import com.kavesh.mc.ui.theme.*
import com.kavesh.mc.util.NetworkUtils
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: KaveshViewModel,
    onOpenFiles: (ServerConfig) -> Unit,
    onOpenSettings: (ServerConfig) -> Unit,
    onOpenConsole: () -> Unit
) {
    val servers by viewModel.servers.collectAsState()
    val status by viewModel.status.collectAsState()
    val activeId by viewModel.activeServerId.collectAsState()
    val localAddress by viewModel.localAddress.collectAsState()
    val publicAddress by viewModel.publicAddress.collectAsState()
    val tunnelMode by viewModel.tunnelMode.collectAsState()

    var showTunnelMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "kavesh-mc",
                color = EmeraldPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .clickable { showTunnelMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Language, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Text(tunnelMode.badge, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Outlined.ArrowDropDown, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = showTunnelMenu,
                        onDismissRequest = { showTunnelMenu = false },
                        modifier = Modifier.background(CardSurfaceElevated)
                    ) {
                        TunnelMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        mode.label,
                                        color = if (mode == tunnelMode) EmeraldPrimary else TextPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.setTunnelMode(mode)
                                    showTunnelMenu = false
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CardSurface)
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clickable {
                            Toast.makeText(context, "Kavesh-MC Self-Hosted Engine (No 429 Limits)", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Forum, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            items(servers, key = { it.id }) { server ->
                val isThisActive = activeId == server.id
                val serverStatus = if (isThisActive) status else ServerStatus.OFFLINE
                val ipToShow = if (isThisActive) {
                    publicAddress ?: localAddress ?: "${NetworkUtils.getLocalIpAddress()}:${server.port}"
                } else {
                    "${NetworkUtils.getLocalIpAddress()}:${server.port}"
                }

                ServerDashboardCard(
                    modifier = Modifier.animateItem(),
                    server = server,
                    status = serverStatus,
                    ipAddress = ipToShow,
                    isPublicTunnel = isThisActive && publicAddress != null,
                    onStartStop = {
                        if (serverStatus == ServerStatus.OFFLINE) {
                            viewModel.startServer(server)
                        } else {
                            viewModel.stopServer()
                        }
                    },
                    onOpenFiles = { onOpenFiles(server) },
                    onOpenSettings = { onOpenSettings(server) },
                    onDelete = { viewModel.deleteServer(server.id) },
                    onOpenConsole = onOpenConsole
                )
            }
        }
    }
}

@Composable
private fun ServerDashboardCard(
    modifier: Modifier = Modifier,
    server: ServerConfig,
    status: ServerStatus,
    ipAddress: String,
    isPublicTunnel: Boolean,
    onStartStop: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onDelete: () -> Unit,
    onOpenConsole: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    val statusColor by animateColorAsState(
        targetValue = when (status) {
            ServerStatus.ONLINE -> EmeraldPrimary
            ServerStatus.STARTING, ServerStatus.STOPPING -> WarnAmber
            ServerStatus.OFFLINE -> TextMuted
        },
        animationSpec = tween(300),
        label = "statusColor"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (status == ServerStatus.ONLINE) EmeraldPrimary.copy(alpha = 0.55f) else BorderSubtle,
        animationSpec = tween(350),
        label = "cardBorder"
    )

    val pulse = rememberInfiniteTransition(label = "dotPulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (status == ServerStatus.OFFLINE) 1f else 0.25f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "dotAlpha"
    )

    val startInteraction = remember { MutableInteractionSource() }
    val startPressed by startInteraction.collectIsPressedAsState()
    val startScale by animateFloatAsState(
        targetValue = if (startPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "startScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(dotAlpha)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = status.name,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            AnimatedVisibility(visible = status != ServerStatus.OFFLINE, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = "View Console →",
                    color = EmeraldPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onOpenConsole() }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Icon + Server Name + Software Metadata
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            KaveshBrandSquare(size = 48.dp)
            Column {
                Text(server.name, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${server.software} ${server.version} • Max ${server.performance.maxPlayers} players",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Live IP Address Strip with smooth highlight animation
        val ipBg by animateColorAsState(
            targetValue = if (status != ServerStatus.OFFLINE) EmeraldDarkBg else CardSurfaceElevated,
            animationSpec = tween(300),
            label = "ipBg"
        )
        val ipBorder by animateColorAsState(
            targetValue = if (status != ServerStatus.OFFLINE) EmeraldPrimary.copy(alpha = 0.45f) else BorderSubtle,
            animationSpec = tween(300),
            label = "ipBorder"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ipBg)
                .border(1.dp, ipBorder, RoundedCornerShape(12.dp))
                .clickable {
                    clipboard.setText(AnnotatedString(ipAddress))
                    copied = true
                    Toast.makeText(context, "Copied IP: $ipAddress", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isPublicTunnel) "PUBLIC TUNNEL IP" else "SERVER IP (LOCAL LAN)",
                    color = if (status != ServerStatus.OFFLINE) EmeraldBright else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = ipAddress,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                contentDescription = "Copy IP",
                tint = if (copied || status != ServerStatus.OFFLINE) EmeraldBright else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        // Action Row: START/STOP + Folder + Settings + Trash
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isRunning = status != ServerStatus.OFFLINE
            val btnBg by animateColorAsState(if (isRunning) ErrorBg else CardSurfaceElevated, tween(250), label = "btnBg")
            val btnBorder by animateColorAsState(if (isRunning) ErrorRed.copy(alpha = 0.6f) else BorderSubtle, tween(250), label = "btnBorder")

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .scale(startScale)
                    .clip(RoundedCornerShape(22.dp))
                    .background(btnBg)
                    .border(1.dp, btnBorder, RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = startInteraction,
                        indication = null
                    ) { onStartStop() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = if (isRunning) ErrorRed else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (status) {
                        ServerStatus.OFFLINE -> "START"
                        ServerStatus.STARTING -> "STARTING..."
                        ServerStatus.ONLINE -> "STOP"
                        ServerStatus.STOPPING -> "STOPPING..."
                    },
                    color = if (isRunning) ErrorRed else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            SmallActionButton(Icons.Outlined.Folder, onClick = onOpenFiles)
            SmallActionButton(Icons.Outlined.Settings, onClick = onOpenSettings)
            SmallActionButton(Icons.Outlined.DeleteOutline, onClick = onDelete)
        }
    }
}

@Composable
private fun SmallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurfaceElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
    }
}
