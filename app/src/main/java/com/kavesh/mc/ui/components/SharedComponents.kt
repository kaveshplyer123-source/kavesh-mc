package com.kavesh.mc.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavesh.mc.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun KaveshBrandSquare(size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF38D373), Color(0xFF1F9D52)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "kavesh",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.22f).sp,
                lineHeight = (size.value * 0.24f).sp
            )
            Text(
                text = "mc",
                color = Color(0xFFD1FAE5),
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.24f).sp,
                lineHeight = (size.value * 0.24f).sp
            )
        }
    }
}

@Composable
fun TypewriterSplashScreen(onFinished: () -> Unit) {
    val word = "kavesh"
    var typedCount by remember { mutableIntStateOf(0) }
    var showMc by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    LaunchedEffect(Unit) {
        delay(220)
        for (i in 1..word.length) {
            typedCount = i
            delay(95)
        }
        delay(120)
        showMc = true
        delay(750)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF173021), BgObsidian),
                    radius = 900f * glowScale
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = word.take(typedCount),
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(4.dp)
                        .height(42.dp)
                        .alpha(cursorAlpha)
                        .background(EmeraldBright, RoundedCornerShape(2.dp))
                )
            }
            AnimatedVisibility(
                visible = showMc,
                enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 2 }
            ) {
                Text(
                    text = "mc",
                    color = EmeraldPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun FloatingBottomNav(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onCreateClick: () -> Unit
) {
    val createInteraction = remember { MutableInteractionSource() }
    val createPressed by createInteraction.collectIsPressedAsState()
    val createScale by animateFloatAsState(
        targetValue = if (createPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "createScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(CardSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIconItem(Icons.Outlined.Home, selectedTab == 0) { onSelectTab(0) }
            NavIconItem(Icons.Outlined.Terminal, selectedTab == 1) { onSelectTab(1) }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(createScale)
                    .clip(CircleShape)
                    .background(EmeraldPrimary)
                    .clickable(
                        interactionSource = createInteraction,
                        indication = null
                    ) { onCreateClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Create Server",
                    tint = Color(0xFF091A0E),
                    modifier = Modifier.size(28.dp)
                )
            }

            NavIconItem(Icons.Outlined.Folder, selectedTab == 2) { onSelectTab(2) }
            NavIconItem(Icons.Outlined.Person, selectedTab == 3) { onSelectTab(3) }
        }
    }
}

@Composable
private fun NavIconItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navScale"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) EmeraldPrimary else TextMuted,
        animationSpec = tween(220),
        label = "navTint"
    )

    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(26.dp)
                .scale(scale)
        )
    }
}
