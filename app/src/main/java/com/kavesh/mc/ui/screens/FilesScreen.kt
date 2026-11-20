package com.kavesh.mc.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavesh.mc.ui.KaveshViewModel
import com.kavesh.mc.ui.theme.*
import java.io.File

@Composable
fun FilesScreen(viewModel: KaveshViewModel, initialDir: File? = null) {
    val rootDir = viewModel.getServersRootDir()
    var currentDir by remember(initialDir) { mutableStateOf(initialDir ?: rootDir) }
    var search by remember { mutableStateOf("") }
    var editingFile by remember { mutableStateOf<File?>(null) }
    var editingText by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val context = LocalContext.current

    val files = remember(currentDir, search, refreshTrigger) {
        val list = currentDir.listFiles()?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name }) ?: emptyList()
        if (search.isBlank()) list else list.filter { it.name.contains(search, ignoreCase = true) }
    }

    if (editingFile != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgObsidian)
                .padding(16.dp)
                .padding(bottom = 86.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { editingFile = null }) {
                        Icon(Icons.Outlined.ArrowBack, null, tint = TextPrimary)
                    }
                    Text(editingFile!!.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Button(
                    onClick = {
                        editingFile!!.writeText(editingText)
                        Toast.makeText(context, "Saved ${editingFile!!.name}", Toast.LENGTH_SHORT).show()
                        editingFile = null
                        refreshTrigger++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save", color = Color(0xFF0A1A0F), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = editingText,
                onValueChange = { editingText = it },
                modifier = Modifier.fillMaxSize(),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text("Files", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextMuted) },
            placeholder = { Text("Search files...", color = TextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Breadcrumb
        val relPath = currentDir.absolutePath.removePrefix(rootDir.absolutePath).trimStart('/')
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Home",
                color = EmeraldPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable { currentDir = rootDir }
            )
            if (relPath.isNotEmpty()) {
                Text(" > $relPath", color = TextMuted, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            if (currentDir != rootDir) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { currentDir = currentDir.parentFile ?: rootDir }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.DriveFileMove, null, tint = TextMuted)
                        Text(".. (Back)", color = TextMuted, fontWeight = FontWeight.Medium)
                    }
                }
            }

            items(files, key = { it.absolutePath }) { file ->
                var showMenu by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .clickable {
                            if (file.isDirectory) {
                                currentDir = file
                            } else {
                                editingText = file.readText()
                                editingFile = file
                            }
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description,
                            contentDescription = null,
                            tint = if (file.isDirectory) EmeraldPrimary else TextMuted,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(file.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            val subtitle = if (file.isDirectory) {
                                "${file.listFiles()?.size ?: 0} items"
                            } else {
                                "${file.length()} bytes"
                            }
                            Text(subtitle, color = TextMuted, fontSize = 12.sp)
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, null, tint = TextMuted)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(CardSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = ErrorRed) },
                                onClick = {
                                    file.deleteRecursively()
                                    showMenu = false
                                    refreshTrigger++
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
