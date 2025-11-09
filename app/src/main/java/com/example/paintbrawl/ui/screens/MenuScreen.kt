package com.example.paintbrawl.ui.screens

import android.bluetooth.BluetoothAdapter
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    onStartGame: (GameMode, Boolean) -> Unit,  // Agregado parámetro isHost
    onShowStats: () -> Unit,
    bluetoothAdapter: BluetoothAdapter?
) {
    var showModeSelection by remember { mutableStateOf(false) }
    var showBluetoothDialog by remember { mutableStateOf(false) }

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

            // Botones del menú
            AnimatedVisibility(
                visible = !showModeSelection,
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
                visible = showModeSelection,
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
                        onClick = {
                            // Aquí se debe mostrar diálogo para elegir si es host o cliente
                            showBluetoothDialog = true
                        },
                        enabled = bluetoothAdapter?.isEnabled == true
                    )

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
                visible = showBluetoothDialog,
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
                            onStartGame(GameMode.BLUETOOTH, true)
                        }
                    )

                    MenuButton(
                        text = "Unirse a Partida (Cliente)",
                        subtitle = "Conectarse como jugador 2",
                        onClick = {
                            onStartGame(GameMode.BLUETOOTH, false)
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