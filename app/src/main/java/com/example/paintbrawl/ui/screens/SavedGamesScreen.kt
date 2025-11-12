package com.example.paintbrawl.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paintbrawl.model.SaveFormat
import com.example.paintbrawl.model.SavedGameMetadata
import com.example.paintbrawl.viewmodel.SavedGamesViewModel
import com.example.paintbrawl.viewmodel.SavedGamesViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedGamesScreen(
    onBack: () -> Unit,
    onLoadGame: (String) -> Unit,
    viewModel: SavedGamesViewModel = viewModel(
        factory = SavedGamesViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
    )
) {
    val savedGames by viewModel.savedGames.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    var showFileContent by remember { mutableStateOf<String?>(null) }
    var fileToDelete by remember { mutableStateOf<String?>(null) }
    var showFormatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSavedGames()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF311B92)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Partidas Guardadas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showFormatDialog = true }) {
                        Icon(Icons.Default.Settings, "Configuración")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )

            // Filtro por formato
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFormat == null,
                    onClick = { viewModel.filterByFormat(null) },
                    label = { Text("Todos") }
                )
                SaveFormat.values().forEach { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { viewModel.filterByFormat(format) },
                        label = { Text(format.name) }
                    )
                }
            }

            if (savedGames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay partidas guardadas",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedGames) { metadata ->
                        SavedGameCard(
                            metadata = metadata,
                            onLoad = { onLoadGame(metadata.fileName) },
                            onDelete = { fileToDelete = metadata.fileName },
                            onExport = { viewModel.exportGame(metadata.fileName) },
                            onViewContent = {
                                viewModel.getFileContent(metadata.fileName) { content ->
                                    showFileContent = content
                                }
                            }
                        )
                    }
                }
            }
        }

        // Diálogo para ver contenido del archivo
        showFileContent?.let { content ->
            AlertDialog(
                onDismissRequest = { showFileContent = null },
                title = { Text("Contenido del Archivo") },
                text = {
                    LazyColumn {
                        item {
                            Text(
                                text = content,
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFileContent = null }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        // Diálogo de confirmación de eliminación
        fileToDelete?.let { fileName ->
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text("Eliminar Partida") },
                text = { Text("¿Estás seguro de que deseas eliminar esta partida? Esta acción no se puede deshacer.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteGame(fileName)
                            fileToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { fileToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo de configuración de formato
        if (showFormatDialog) {
            AlertDialog(
                onDismissRequest = { showFormatDialog = false },
                title = { Text("Formato Preferido de Guardado") },
                text = {
                    Column {
                        SaveFormat.values().forEach { format ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setPreferredFormat(format)
                                        showFormatDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = viewModel.preferredFormat.value == format,
                                    onClick = {
                                        viewModel.setPreferredFormat(format)
                                        showFormatDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(format.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        when (format) {
                                            SaveFormat.TXT -> "Texto plano, fácil de leer"
                                            SaveFormat.XML -> "Formato estructurado XML"
                                            SaveFormat.JSON -> "Formato JSON, compacto"
                                        },
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFormatDialog = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}

@Composable
fun SavedGameCard(
    metadata: SavedGameMetadata,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onViewContent: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata.gameName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormat.format(Date(metadata.timestamp)),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (metadata.format) {
                        SaveFormat.TXT -> Color(0xFF4CAF50)
                        SaveFormat.XML -> Color(0xFF2196F3)
                        SaveFormat.JSON -> Color(0xFFFF9800)
                    }
                ) {
                    Text(
                        text = metadata.format.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Modo: ${if (metadata.gameMode == "CLARIVIDENTE") "Clarividente" else "2 Jugadores"}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = metadata.player1Name + (metadata.player2Name?.let { " vs $it" } ?: ""),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            if (metadata.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    metadata.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onLoad,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            "Cargar",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            "Más opciones",
                            tint = Color.White
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = Color.White.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = onViewContent) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver", color = Color.White)
                        }
                        TextButton(onClick = onExport) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar", color = Color.White)
                        }
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar", color = Color(0xFFFF5252))
                        }
                    }
                }
            }
        }
    }
}