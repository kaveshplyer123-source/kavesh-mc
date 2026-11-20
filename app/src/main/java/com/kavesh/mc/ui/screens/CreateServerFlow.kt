package com.kavesh.mc.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavesh.mc.model.*
import com.kavesh.mc.ui.KaveshViewModel
import com.kavesh.mc.ui.theme.*
import com.kavesh.mc.util.ModrinthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class SetupPage {
    HUB, FULL_SETUP, GAMEPLAY, PERFORMANCE, MODS, BENCHMARK
}

@Composable
fun CreateServerFlow(
    viewModel: KaveshViewModel,
    initialConfig: ServerConfig? = null,
    onClose: () -> Unit
) {
    var page by remember { mutableStateOf(if (initialConfig != null) SetupPage.FULL_SETUP else SetupPage.HUB) }
    var draft by remember { mutableStateOf(initialConfig ?: ServerConfig(name = "")) }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 8 }) togetherWith
                (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 8 })
        },
        label = "setupPageTransition"
    ) { currentPage ->
    when (currentPage) {
        SetupPage.HUB -> CreateServerHubScreen(
            onBack = onClose,
            onGuidedOrFull = { page = SetupPage.FULL_SETUP },
            onMods = { page = SetupPage.MODS },
            onBenchmark = { page = SetupPage.BENCHMARK }
        )
        SetupPage.FULL_SETUP -> FullSetupScreen(
            draft = draft,
            isEditing = initialConfig != null,
            onUpdateDraft = { draft = it },
            onBack = { if (initialConfig != null) onClose() else page = SetupPage.HUB },
            onOpenGameplay = { page = SetupPage.GAMEPLAY },
            onOpenPerformance = { page = SetupPage.PERFORMANCE },
            onOpenMods = { page = SetupPage.MODS },
            onSave = {
                val finalCfg = draft.copy(name = draft.name.ifBlank { "Smp Server" })
                viewModel.addOrUpdateServer(finalCfg)
                onClose()
            }
        )
        SetupPage.GAMEPLAY -> GameplaySettingsScreen(
            gameplay = draft.gameplay,
            onUpdate = { draft = draft.copy(gameplay = it) },
            onDone = { page = SetupPage.FULL_SETUP }
        )
        SetupPage.PERFORMANCE -> PerformanceSettingsScreen(
            perf = draft.performance,
            onUpdate = { draft = draft.copy(performance = it) },
            onDone = { page = SetupPage.FULL_SETUP }
        )
        SetupPage.MODS -> ModrinthBrowserScreen(
            targetModsDir = File(viewModel.getServersRootDir(), "${draft.id}/plugins"),
            onDone = { page = SetupPage.FULL_SETUP }
        )
        SetupPage.BENCHMARK -> BenchmarkScreen(
            onBack = { page = SetupPage.HUB }
        )
    }
    }
}

@Composable
private fun CreateServerHubScreen(
    onBack: () -> Unit,
    onGuidedOrFull: () -> Unit,
    onMods: () -> Unit,
    onBenchmark: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TopBackHeader("Create Server", onBack)
        Spacer(Modifier.height(16.dp))
        Text("How do you want to get started?", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
        Spacer(Modifier.height(6.dp))
        Text("Choose your setup type, you can change everything later.", color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        // Featured Guided Setup Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(20.dp))
                .clickable { onGuidedOrFull() }
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(EmeraldBright),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Timer, null, tint = Color(0xFF0A1A0F), modifier = Modifier.size(26.dp))
                    }
                    Column {
                        Text("Guided Setup", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(EmeraldDarkBg).padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Recommended", color = EmeraldBright, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("~2 minutes", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
                Icon(Icons.Outlined.ArrowForward, null, tint = EmeraldPrimary)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Step-by-step wizard. We'll walk you through naming your server, picking software, and setting RAM.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }

        Spacer(Modifier.height(22.dp))
        Text("Start from a template", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardSurface).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
        ) {
            SetupOptionRow(Icons.Outlined.Download, Color(0xFF3A2F18), Color(0xFFEAB308), "Modpack Setup", "A modpack from Modrinth", onMods)
            HorizontalDivider(color = BorderSubtle)
            SetupOptionRow(Icons.Outlined.SportsEsports, Color(0xFF2B1F3B), Color(0xFFA855F7), "Gamemode Setup", "Paper + a ready-made gamemode", onGuidedOrFull)
        }

        Spacer(Modifier.height(22.dp))
        Text("More options", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardSurface).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
        ) {
            SetupOptionRow(Icons.Outlined.Settings, Color(0xFF1D2939), Color(0xFF60A5FA), "Full Setup", "All options at once", onGuidedOrFull)
            HorizontalDivider(color = BorderSubtle)
            SetupOptionRow(Icons.Outlined.Restore, Color(0xFF382315), Color(0xFFF97316), "Restore from Backup", "From local or cloud backup") {
                Toast.makeText(context, "Place backup ZIP in Files tab to restore", Toast.LENGTH_SHORT).show()
            }
            HorizontalDivider(color = BorderSubtle)
            SetupOptionRow(Icons.Outlined.Speed, CardSurfaceElevated, TextPrimary, "Benchmark", "Score your phone's hosting power", onBenchmark)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun FullSetupScreen(
    draft: ServerConfig,
    isEditing: Boolean,
    onUpdateDraft: (ServerConfig) -> Unit,
    onBack: () -> Unit,
    onOpenGameplay: () -> Unit,
    onOpenPerformance: () -> Unit,
    onOpenMods: () -> Unit,
    onSave: () -> Unit
) {
    val softwares = listOf("Pumpkin" to "26.3", "Paper" to "1.21.4", "Purpur" to "1.21.4", "Vanilla" to "26.3")
    var showSoftwareDialog by remember { mutableStateOf(false) }

    if (showSoftwareDialog) {
        AlertDialog(
            onDismissRequest = { showSoftwareDialog = false },
            containerColor = CardSurface,
            title = { Text("Select Software", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    softwares.forEach { (sw, ver) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (draft.software == sw) EmeraldDarkBg else CardSurfaceElevated)
                                .clickable {
                                    onUpdateDraft(draft.copy(software = sw, version = ver, eulaAccepted = true))
                                    showSoftwareDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$sw $ver", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            if (draft.software == sw) Text("Selected", color = EmeraldPrimary, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoftwareDialog = false }) { Text("Close", color = EmeraldPrimary) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BgObsidian)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 90.dp)
        ) {
            TopBackHeader(if (isEditing) "Server Settings" else "New Server", onBack)
            Spacer(Modifier.height(16.dp))

            Text("GET STARTED", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardSurface).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)).padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepBadge("1")
                    Text("Server Name", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onUpdateDraft(draft.copy(name = it)) },
                    placeholder = { Text("Server name *", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showSoftwareDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StepBadge("2")
                        Column {
                            Text("Software", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("${draft.software} ${draft.version} • EULA Accepted", color = EmeraldPrimary, fontSize = 13.sp)
                        }
                    }
                    Icon(Icons.Outlined.ArrowForward, null, tint = TextMuted)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("GAME", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardSurface).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            ) {
                SetupOptionRow(Icons.Outlined.SportsEsports, CardSurfaceElevated, EmeraldPrimary, "Gameplay", "${draft.gameplay.gamemode} · ${draft.gameplay.difficulty}", onOpenGameplay)
                HorizontalDivider(color = BorderSubtle)
                SetupOptionRow(Icons.Outlined.Extension, CardSurfaceElevated, EmeraldPrimary, "Mods & Plugins", "Browse Modrinth", onOpenMods)
            }

            Spacer(Modifier.height(20.dp))
            Text("SERVER", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardSurface).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            ) {
                SetupOptionRow(
                    Icons.Outlined.Memory,
                    CardSurfaceElevated,
                    EmeraldPrimary,
                    "Performance",
                    "${draft.performance.allocatedRamGb} GB RAM · View ${draft.performance.viewDistance} · Sim ${draft.performance.simulationDistance}",
                    onOpenPerformance
                )
                HorizontalDivider(color = BorderSubtle)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Online Mode (Premium Auth)", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Disable for offline / cracked clients", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = draft.onlineMode,
                        onCheckedChange = { onUpdateDraft(draft.copy(onlineMode = it)) },
                        colors = SwitchDefaults.colors(checkedTrackColor = EmeraldPrimary)
                    )
                }
            }
        }

        StickyGreenButton(
            label = if (isEditing) "Save changes" else "Create server",
            modifier = Modifier.align(Alignment.BottomCenter),
            onClick = onSave
        )
    }
}

@Composable
private fun GameplaySettingsScreen(
    gameplay: GameplaySettings,
    onUpdate: (GameplaySettings) -> Unit,
    onDone: () -> Unit
) {
    val gamemodes = listOf("Survival", "Creative", "Adventure", "Spectator")
    val difficulties = listOf("Peaceful", "Easy", "Normal", "Hard")

    Box(modifier = Modifier.fillMaxSize().background(BgObsidian)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 90.dp)
        ) {
            TopBackHeader("Gameplay", onDone)
            Spacer(Modifier.height(8.dp))
            Text("ⓘ These apply the next time the server starts", color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))

            // WORLD & DIFFICULTY
            SettingsSectionCard(Icons.Outlined.Settings, "WORLD & DIFFICULTY") {
                CycleSettingRow("Gamemode", gameplay.gamemode) {
                    val next = gamemodes[(gamemodes.indexOf(gameplay.gamemode) + 1) % gamemodes.size]
                    onUpdate(gameplay.copy(gamemode = next))
                }
                HorizontalDivider(color = BorderSubtle)
                CycleSettingRow("Difficulty", gameplay.difficulty) {
                    val next = difficulties[(difficulties.indexOf(gameplay.difficulty) + 1) % difficulties.size]
                    onUpdate(gameplay.copy(difficulty = next))
                }
                HorizontalDivider(color = BorderSubtle)
                ToggleSettingRow("Hardcore Mode", "Players banned on death", gameplay.hardcore) {
                    onUpdate(gameplay.copy(hardcore = it))
                }
            }

            Spacer(Modifier.height(14.dp))

            // WORLD RULES
            SettingsSectionCard(Icons.Outlined.SportsEsports, "WORLD RULES") {
                Text("Presets", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Default", "Relaxed", "Builder").forEach { preset ->
                        val active = gameplay.preset == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (active) EmeraldDarkBg else CardSurfaceElevated)
                                .border(1.dp, if (active) EmeraldPrimary else BorderSubtle, RoundedCornerShape(20.dp))
                                .clickable {
                                    onUpdate(
                                        when (preset) {
                                            "Relaxed" -> gameplay.copy(preset = preset, keepInventory = true, mobGriefing = false)
                                            "Builder" -> gameplay.copy(preset = preset, gamemode = "Creative", mobGriefing = false)
                                            else -> gameplay.copy(preset = preset, keepInventory = false, mobGriefing = true)
                                        }
                                    )
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset, color = if (active) EmeraldBright else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ToggleSettingRow("Keep Items On Death", "Players keep what they carry when they die", gameplay.keepInventory) {
                    onUpdate(gameplay.copy(keepInventory = it))
                }
                HorizontalDivider(color = BorderSubtle)
                ToggleSettingRow("Mob Griefing", "Allow creepers and endermen to break blocks", gameplay.mobGriefing) {
                    onUpdate(gameplay.copy(mobGriefing = it))
                }
                HorizontalDivider(color = BorderSubtle)
                ToggleSettingRow("Command Blocks", "Let command blocks run in the world", gameplay.commandBlocks) {
                    onUpdate(gameplay.copy(commandBlocks = it))
                }
                HorizontalDivider(color = BorderSubtle)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Spawn Protection", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("${gameplay.spawnProtection} blocks", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = gameplay.spawnProtection.toFloat(),
                    onValueChange = { onUpdate(gameplay.copy(spawnProtection = it.roundToInt())) },
                    valueRange = 0f..64f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = EmeraldPrimary)
                )
            }

            Spacer(Modifier.height(14.dp))

            // RESOURCE PACK
            SettingsSectionCard(Icons.Outlined.Link, "RESOURCE PACK") {
                Text("Link to a pack players get when they join", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = gameplay.resourcePackUrl,
                    onValueChange = { onUpdate(gameplay.copy(resourcePackUrl = it)) },
                    placeholder = { Text("https://example.com/pack.zip", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
        StickyGreenButton("Done", Modifier.align(Alignment.BottomCenter), onDone)
    }
}

@Composable
private fun PerformanceSettingsScreen(
    perf: PerformanceSettings,
    onUpdate: (PerformanceSettings) -> Unit,
    onDone: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(BgObsidian)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 90.dp)
        ) {
            TopBackHeader("Performance", onDone)
            Spacer(Modifier.height(16.dp))

            SettingsSectionCard(Icons.Outlined.Memory, "MEMORY") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Allocated RAM", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("${String.format("%.1f", perf.allocatedRamGb)} GB", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = perf.allocatedRamGb,
                    onValueChange = { onUpdate(perf.copy(allocatedRamGb = (it * 2).roundToInt() / 2f)) },
                    valueRange = 0.5f..6.0f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = EmeraldPrimary)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0.5 GB", color = TextMuted, fontSize = 12.sp)
                    Text("6.0 GB", color = TextMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = BorderSubtle)
                ToggleSettingRow("Performance Mode", "Allocates more memory up front for smoother performance.", perf.performanceMode) {
                    onUpdate(perf.copy(performanceMode = it))
                }
            }

            Spacer(Modifier.height(14.dp))

            SettingsSectionCard(Icons.Outlined.Map, "WORLD LOADING") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("View Distance", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("${perf.viewDistance} chunks", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = perf.viewDistance.toFloat(),
                    onValueChange = { onUpdate(perf.copy(viewDistance = it.roundToInt())) },
                    valueRange = 2f..16f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = EmeraldPrimary)
                )
                HorizontalDivider(color = BorderSubtle)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Simulation Distance", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("${perf.simulationDistance} chunks", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = perf.simulationDistance.toFloat(),
                    onValueChange = { onUpdate(perf.copy(simulationDistance = it.roundToInt())) },
                    valueRange = 2f..12f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = EmeraldPrimary)
                )
            }

            Spacer(Modifier.height(14.dp))

            SettingsSectionCard(Icons.Outlined.Dns, "SERVER") {
                ToggleSettingRow("Optimize for phone", "Tunes game settings (mobs, redstone, autosave) to run smoother on your phone", perf.optimizeForPhone) {
                    onUpdate(perf.copy(optimizeForPhone = it))
                }
                HorizontalDivider(color = BorderSubtle)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Max Players", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("${perf.maxPlayers} players", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = perf.maxPlayers.toFloat(),
                    onValueChange = { onUpdate(perf.copy(maxPlayers = it.roundToInt())) },
                    valueRange = 1f..20f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = EmeraldPrimary)
                )
            }
        }
        StickyGreenButton("Done", Modifier.align(Alignment.BottomCenter), onDone)
    }
}

@Composable
private fun ModrinthBrowserScreen(targetModsDir: File, onDone: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var mods by remember { mutableStateOf<List<ModrinthProject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(query, page) {
        loading = true
        try {
            val (results, total) = ModrinthClient.searchProjects(query, page)
            mods = results
            totalPages = total
        } catch (e: Exception) {
            Toast.makeText(context, "Modrinth error: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            loading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgObsidian)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 86.dp)
        ) {
            TopBackHeader("Mods & Plugins", onDone)
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; page = 1 },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextMuted) },
                    placeholder = { Text("Search mods on Modrinth", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(12.dp))

                if (loading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mods, key = { it.projectId }) { mod ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(mod.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(mod.description, color = TextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CardSurfaceElevated)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                            .clickable {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://modrinth.com/project/${mod.slug}"))
                                                context.startActivity(intent)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.OpenInNew, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(EmeraldDarkBg)
                                            .border(1.dp, EmeraldPrimary, RoundedCornerShape(10.dp))
                                            .clickable {
                                                scope.launch {
                                                    try {
                                                        val file = ModrinthClient.downloadLatestVersion(mod.projectId, targetModsDir)
                                                        Toast.makeText(context, "Downloaded $file", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Download, null, tint = EmeraldBright, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { if (page > 1) page-- }, enabled = page > 1) { Text("←", color = TextPrimary) }
                    Text("Page $page / $totalPages", color = TextMuted, fontSize = 13.sp)
                    OutlinedButton(onClick = { if (page < totalPages) page++ }, enabled = page < totalPages) { Text("→", color = TextPrimary) }
                }
            }
        }
        StickyGreenButton("Done", Modifier.align(Alignment.BottomCenter), onDone)
    }
}

@Composable
private fun BenchmarkScreen(onBack: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(BgObsidian).padding(16.dp)) {
        TopBackHeader("Benchmark", onBack)
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardSurface).border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Speed, null, tint = EmeraldPrimary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Phone Hosting Benchmark", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("CPU Cores: ${Runtime.getRuntime().availableProcessors()} • Max Heap: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB", color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            if (score != null) {
                Text("$score pts", color = EmeraldBright, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                Text("Recommended: PumpkinMC 26.3 • View 6–8", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    running = true
                    scope.launch {
                        val ms = withContext(Dispatchers.Default) {
                            val start = System.nanoTime()
                            var acc = 0L
                            for (i in 1..4_000_000) acc += (i * 31L) xor (i.toLong() shl 3)
                            ((System.nanoTime() - start) / 1_000_000L).coerceAtLeast(1)
                        }
                        score = (100000 / ms).toInt().coerceIn(500, 9999)
                        running = false
                    }
                },
                enabled = !running,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (running) "Benchmarking..." else "Run Benchmark", color = Color(0xFF0A1A0F), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TopBackHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, null, tint = TextPrimary)
        }
        Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepBadge(num: String) {
    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape).background(CardSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        Text(num, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SetupOptionRow(icon: ImageVector, iconBg: Color, iconTint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, color = TextMuted, fontSize = 12.sp)
            }
        }
        Icon(Icons.Outlined.ArrowForward, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsSectionCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardSurface).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun CycleSettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(CardSurfaceElevated).border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("$value ▾", color = TextPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ToggleSettingRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = EmeraldPrimary)
        )
    }
}

@Composable
private fun StickyGreenButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BgObsidian.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text(label, color = Color(0xFF0A1A0F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
