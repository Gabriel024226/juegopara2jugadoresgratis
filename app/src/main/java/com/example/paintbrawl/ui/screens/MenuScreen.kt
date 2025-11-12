package com.example.paintbrawl.ui.screens

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paintbrawl.model.GameMode
import com.example.paintbrawl.model.ThemeColor

@Composable
fun MenuScreen(
    onStartGame: (GameMode, Boolean, ThemeColor) -> Unit,
    onShowStats: () -> Unit,
    onShowSavedGames: () -> Unit,
    onShowSettings: () -> Unit,
    bluetoothAdapter: BluetoothAdapter?,
    onStartBluetoothServer: () -> Unit = {},
    onConnectToDevice: (BluetoothDevice) -> Unit = {},
    getPairedDevices: () -> Set<BluetoothDevice> = { emptySet() },
    currentTheme: ThemeColor = ThemeColor.AZUL
) {
    var showModeSelection by remember { mutableStateOf(false) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showDeviceSelection by remember { mutableStateOf(false) }
    var isWaitingForConnection by remember { mutableStateOf(false) }
    var showThemeSelection by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    var selectedGameMode by remember { mutableStateOf<GameMode?>(null) } // NUEVO: Guardar modo seleccionado

    val gradient = when (selectedTheme) {
        ThemeColor.GUINDA -> listOf(Color(0xFF6C1D45), Color(0xFF9B2D5E))
        ThemeColor.AZUL -> listOf(Color(0xFF1A237E), Color(0xFF311B92))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradient)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Título del juego
            Text(
                text = "MEMORAMA",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Encuentra las parejas",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botones del menú principal
            AnimatedVisibility(
                visible = !showModeSelection && !showBluetoothDialog && !showDeviceSelection && !isWaitingForConnection && !showThemeSelection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MenuButton(
                        text = "Jugar",
                        icon = Icons.Default.PlayArrow,
                        onClick = { showModeSelection = true }
                    )

                    MenuButton(
                        text = "Cargar Partida",
                        icon = Icons.Default.FolderOpen,
                        onClick = onShowSavedGames
                    )

                    MenuButton(
                        text = "Estadísticas",
                        icon = Icons.Default.BarChart,
                        onClick = onShowStats
                    )

                    MenuButton(
                        text = "Configuración",
                        icon = Icons.Default.Settings,
                        onClick = onShowSettings
                    )
                }
            }

            // Selección de modo de juego
            AnimatedVisibility(
                visible = showModeSelection && !showBluetoothDialog && !showDeviceSelection && !showThemeSelection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Selecciona el modo de juego",
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    MenuButton(
                        text = "Modo Local (2 Jugadores)",
                        subtitle = "Dos jugadores en un dispositivo",
                        icon = Icons.Default.Group,
                        onClick = {
                            selectedGameMode = GameMode.LOCAL
                            showThemeSelection = true
                        }
                    )

                    MenuButton(
                        text = "Modo Clarividente",
                        subtitle = "Un jugador, 24 cartas, 3 vidas",
                        icon = Icons.Default.Visibility,
                        onClick = {
                            selectedGameMode = GameMode.CLARIVIDENTE
                            showThemeSelection = true
                        }
                    )

                    MenuButton(
                        text = "Modo Bluetooth",
                        subtitle = "Jugar por Bluetooth",
                        icon = Icons.Default.Bluetooth,
                        onClick = { showBluetoothDialog = true },
                        enabled = bluetoothAdapter?.isEnabled == true
                    )

                    if (bluetoothAdapter?.isEnabled == false) {
                        Text(
                            text = "⚠️ Bluetooth desactivado",
                            fontSize = 14.sp,
                            color = Color.Yellow,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    TextButton(
                        onClick = { showModeSelection = false },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = "Regresar",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Selección de tema
            AnimatedVisibility(
                visible = showThemeSelection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Selecciona el tema",
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ThemeCard(
                            theme = ThemeColor.GUINDA,
                            selected = selectedTheme == ThemeColor.GUINDA,
                            onClick = { selectedTheme = ThemeColor.GUINDA },
                            modifier = Modifier.weight(1f)
                        )

                        ThemeCard(
                            theme = ThemeColor.AZUL,
                            selected = selectedTheme == ThemeColor.AZUL,
                            onClick = { selectedTheme = ThemeColor.AZUL },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            // CORREGIDO: Usar el modo guardado en selectedGameMode
                            selectedGameMode?.let { mode ->
                                onStartGame(mode, false, selectedTheme)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("Comenzar", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            showThemeSelection = false
                            showModeSelection = true
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Regresar",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Diálogo de selección Bluetooth (Host o Cliente)
            AnimatedVisibility(
                visible = showBluetoothDialog && !showDeviceSelection && !isWaitingForConnection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "¿Cómo quieres conectar?",
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    MenuButton(
                        text = "Crear Partida (Host)",
                        subtitle = "Ser el jugador 1 y esperar conexión",
                        icon = Icons.Default.Wifi,
                        onClick = {
                            isWaitingForConnection = true
                            onStartBluetoothServer()
                            onStartGame(GameMode.BLUETOOTH, true, selectedTheme)
                        }
                    )

                    MenuButton(
                        text = "Unirse a Partida (Cliente)",
                        subtitle = "Conectarse como jugador 2",
                        icon = Icons.Default.ConnectWithoutContact,
                        onClick = {
                            showDeviceSelection = true
                        }
                    )

                    TextButton(
                        onClick = {
                            showBluetoothDialog = false
                            showModeSelection = true
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = "Regresar",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Pantalla de espera de conexión (Host)
            AnimatedVisibility(
                visible = isWaitingForConnection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📡",
                        fontSize = 64.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "Esperando conexión...",
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Text(
                        text = "El otro dispositivo debe unirse a la partida",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    TextButton(
                        onClick = {
                            isWaitingForConnection = false
                            showBluetoothDialog = true
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Selección de dispositivo Bluetooth
            AnimatedVisibility(
                visible = showDeviceSelection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                DeviceSelectionScreen(
                    pairedDevices = getPairedDevices(),
                    onDeviceSelected = { device ->
                        onConnectToDevice(device)
                        onStartGame(GameMode.BLUETOOTH, false, selectedTheme)
                        showDeviceSelection = false
                    },
                    onBack = {
                        showDeviceSelection = false
                        showBluetoothDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun ThemeCard(
    theme: ThemeColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                Color.White.copy(alpha = 0.3f)
            else
                Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (selected)
            androidx.compose.foundation.BorderStroke(3.dp, Color.White)
        else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(theme.primaryColor),
                                    Color(theme.secondaryColor)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (theme) {
                        ThemeColor.GUINDA -> "Guinda"
                        ThemeColor.AZUL -> "Azul"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (selected) {
                    Text(
                        text = "✓",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MenuButton(
    text: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle != null) 80.dp else 60.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.2f),
            contentColor = Color.White,
            disabledContainerColor = Color.Gray.copy(alpha = 0.2f),
            disabledContentColor = Color.Gray
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp,
            disabledElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceSelectionScreen(
    pairedDevices: Set<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Selecciona un dispositivo",
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (pairedDevices.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No hay dispositivos emparejados",
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Empareja dispositivos en la configuración de Bluetooth",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pairedDevices.toList()) { device ->
                    DeviceItem(
                        device = device,
                        onClick = { onDeviceSelected(device) }
                    )
                }
            }
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "Regresar",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📱",
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column {
                Text(
                    text = device.name ?: "Dispositivo desconocido",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = device.address,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}