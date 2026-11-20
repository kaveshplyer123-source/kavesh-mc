package com.kavesh.mc.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavesh.mc.ui.theme.*

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 110.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text("Profile", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // User Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(54.dp).clip(CircleShape).background(EmeraldDarkBg).border(1.5.dp, EmeraldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("K", color = EmeraldBright, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column {
                Text("Kavesh", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(EmeraldDarkBg).padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("SELF-HOSTED PRO • NO LIMITS", color = EmeraldBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        val items = listOf(
            Triple(Icons.Outlined.CloudUpload, "Backups", "Local & Cloud ZIP export"),
            Triple(Icons.Outlined.Computer, "Trusted computers", "LAN Web Console access"),
            Triple(Icons.Outlined.Language, "Language", "System Default"),
            Triple(Icons.Outlined.DarkMode, "Theme", "Obsidian Dark"),
            Triple(Icons.Outlined.HelpOutline, "FAQ & Help", "Bundle libpumpkin.so in jniLibs"),
            Triple(Icons.Outlined.Gavel, "Legal & Licenses", "Open Source & PumpkinMC")
        )

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardSurface).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
        ) {
            items.forEachIndexed { idx, (icon, title, sub) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { Toast.makeText(context, "$title: $sub", Toast.LENGTH_SHORT).show() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(icon, null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(sub, color = TextMuted, fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = TextMuted)
                }
                if (idx < items.lastIndex) HorizontalDivider(color = BorderSubtle)
            }
        }

        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Kavesh-MC v1.0.0", color = TextMuted, fontSize = 12.sp)
        }
    }
}
