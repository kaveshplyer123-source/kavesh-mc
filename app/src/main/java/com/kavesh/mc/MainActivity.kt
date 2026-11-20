package com.kavesh.mc

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kavesh.mc.model.ServerConfig
import com.kavesh.mc.ui.KaveshViewModel
import com.kavesh.mc.ui.components.FloatingBottomNav
import com.kavesh.mc.ui.components.TypewriterSplashScreen
import com.kavesh.mc.ui.screens.*
import com.kavesh.mc.ui.theme.BgObsidian
import com.kavesh.mc.ui.theme.KaveshMCTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            KaveshMCTheme {
                KaveshAppRoot()
            }
        }
    }
}

@Composable
fun KaveshAppRoot(viewModel: KaveshViewModel = viewModel()) {
    var showSplash by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateFlow by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<ServerConfig?>(null) }
    var targetFilesDir by remember { mutableStateOf<File?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
            .safeDrawingPadding()
    ) {
        AnimatedContent(
            targetState = showSplash,
            transitionSpec = { fadeIn(tween(380)) togetherWith fadeOut(tween(320)) },
            label = "splashTransition"
        ) { splash ->
            if (splash) {
                TypewriterSplashScreen(onFinished = { showSplash = false })
            } else {
                Box(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 * dir }) togetherWith
                                (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 12 * dir })
                        },
                        label = "tabTransition"
                    ) { tab ->
                        when (tab) {
                            0 -> HomeScreen(
                                viewModel = viewModel,
                                onOpenFiles = { server ->
                                    targetFilesDir = File(viewModel.getServersRootDir(), server.id)
                                    selectedTab = 2
                                },
                                onOpenSettings = { server ->
                                    editingServer = server
                                },
                                onOpenConsole = { selectedTab = 1 }
                            )
                            1 -> ConsoleScreen(viewModel = viewModel)
                            2 -> FilesScreen(viewModel = viewModel, initialDir = targetFilesDir)
                            3 -> ProfileScreen()
                        }
                    }

                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        FloatingBottomNav(
                            selectedTab = selectedTab,
                            onSelectTab = {
                                if (it != 2) targetFilesDir = null
                                selectedTab = it
                            },
                            onCreateClick = { showCreateFlow = true }
                        )
                    }

                    // Slide-up modal animation for Create Server / Edit Settings
                    AnimatedVisibility(
                        visible = showCreateFlow || editingServer != null,
                        enter = slideInVertically(tween(280)) { it } + fadeIn(tween(220)),
                        exit = slideOutVertically(tween(240)) { it } + fadeOut(tween(200))
                    ) {
                        CreateServerFlow(
                            viewModel = viewModel,
                            initialConfig = editingServer,
                            onClose = {
                                showCreateFlow = false
                                editingServer = null
                            }
                        )
                    }
                }
            }
        }
    }
}
