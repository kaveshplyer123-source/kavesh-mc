package com.kavesh.mc.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavesh.mc.model.LogLevel
import com.kavesh.mc.ui.KaveshViewModel
import com.kavesh.mc.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ConsoleScreen(viewModel: KaveshViewModel) {
    val logs by viewModel.logs.collectAsState()
    val localAddress by viewModel.localAddress.collectAsState()
    val publicAddress by viewModel.publicAddress.collectAsState()
    val status by viewModel.status.collectAsState()

    var commandText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isBlank()) logs
        else logs.filter { it.message.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.lastIndex)
        }
    }

    val quickCommands = listOf(
        "/stop", "/help", "/list", "/op", "/kick", "/ban", "/say", "/time day", "/time night", "/weather clear", "/tp"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 92.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // Header: Console + WEB CONSOLE button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Console", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .clickable {
                        val ip = localAddress ?: "Start server first"
                        Toast.makeText(context, "Server IP: $ip", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.OpenInNew, null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                Text("WEB CONSOLE", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Live IP Strip inside Console
        val activeIp = publicAddress ?: localAddress
        if (activeIp != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldDarkBg)
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(activeIp))
                        Toast.makeText(context, "Copied $activeIp", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${status.name} • $activeIp",
                    color = EmeraldBright,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Rounded.ContentCopy, null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        // LIVE OUTPUT divider + Search + Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("— LIVE OUTPUT —", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = {
                        val allText = logs.joinToString(separator = "\n") { "[${it.timestamp}] [${it.level}] ${it.message}" }
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, allText)
                        }
                        context.startActivity(Intent.createChooser(share, "Share Kavesh-MC Logs"))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Outlined.Share, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter logs...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Quick-Command Chip Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickCommands.forEach { cmd ->
                val isStop = cmd == "/stop"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isStop) ErrorBg else CardSurface)
                        .border(1.dp, if (isStop) ErrorRed.copy(alpha = 0.6f) else BorderSubtle, RoundedCornerShape(16.dp))
                        .clickable {
                            if (cmd in listOf("/op", "/kick", "/ban", "/say", "/tp")) {
                                commandText = "$cmd "
                            } else {
                                viewModel.sendCommand(cmd)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cmd,
                        color = if (isStop) ErrorRed else TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Log Feed
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (filteredLogs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Start your server to see live console output.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val isWarn = log.level == LogLevel.WARN
                        val isErr = log.level == LogLevel.ERROR
                        val cardBg = when {
                            isErr -> ErrorBg
                            isWarn -> WarnBg
                            else -> CardSurface
                        }
                        val borderColor = when {
                            isErr -> ErrorRed.copy(alpha = 0.6f)
                            isWarn -> WarnAmber.copy(alpha = 0.6f)
                            else -> BorderSubtle
                        }
                        val badgeColor = when (log.level) {
                            LogLevel.INFO -> EmeraldPrimary
                            LogLevel.WARN -> WarnAmber
                            LogLevel.ERROR -> ErrorRed
                            LogLevel.SYSTEM -> Color(0xFF60A5FA)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.timestamp, color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    log.level.name,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = log.message,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Floating ↓ Latest Pill
            if (filteredLogs.size > 5) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurfaceElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .clickable {
                            scope.launch { listState.animateScrollToItem(filteredLogs.lastIndex) }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("↓ Latest", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Command Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("/", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            TextField(
                value = commandText,
                onValueChange = { commandText = it },
                placeholder = { Text("Enter command...", color = TextMuted, fontSize = 14.sp) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary)
                    .clickable {
                        viewModel.sendCommand(commandText)
                        commandText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Color(0xFF0A1A0F), modifier = Modifier.size(18.dp))
            }
        }
    }
}
