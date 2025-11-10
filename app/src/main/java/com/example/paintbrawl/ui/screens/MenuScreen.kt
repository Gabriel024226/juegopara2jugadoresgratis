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

/**
 * Pantalla del menú principal
 */
@Composable
fun MenuScreen(
    onStartGame: (GameMode, Boolean) -> Unit,
    onShowStats: () -> Unit,
    bluetoothAdapter: BluetoothAdapter?,
    onStartBluetoothServer: () -> Unit = {},
    onConnectToDevice: (BluetoothDevice) -> Unit = {},
    getPairedDevices: () -> Set<BluetoothDevice> = { emptySet() }
) {
    var showModeSelection by remember { mutableStateOf(false) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showDeviceSelection by remember { mutableStateOf(false) }
    var isWaitingForConnection by remember { mutableStateOf(false) }

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
            ),
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
                visible = !showModeSelection && !showBluetoothDialog && !showDeviceSelection && !isWaitingForConnection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MenuButton(
                        text = "Jugar",
                        onClick = { showModeSelection = true }
                    )

                    MenuButton(
                        text = "Estadísticas",
                        onClick = onShowStats
                    )
                }
            }

            // Selección de modo de juego
            AnimatedVisibility(
                visible = showModeSelection && !showBluetoothDialog && !showDeviceSelection,
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
                        text = "Modo Local",
                        subtitle = "Dos jugadores en un dispositivo",
                        onClick = { onStartGame(GameMode.LOCAL, false) }
                    )

                    MenuButton(
                        text = "Modo Bluetooth",
                        subtitle = "Jugar por Bluetooth",
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
                        onClick = {
                            isWaitingForConnection = true
                            onStartBluetoothServer()
                            onStartGame(GameMode.BLUETOOTH, true)
                        }
                    )

                    MenuButton(
                        text = "Unirse a Partida (Cliente)",
                        subtitle = "Conectarse como jugador 2",
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
                        onStartGame(GameMode.BLUETOOTH, false)
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

/**
 * Pantalla de selección de dispositivos Bluetooth
 */
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

/**
 * Item de dispositivo Bluetooth
 */
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

/**
 * Componente de botón personalizado para el menú
 */
@Composable
fun MenuButton(
    text: String,
    subtitle: String? = null,
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