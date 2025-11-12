package com.example.paintbrawl.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paintbrawl.model.Card
import com.example.paintbrawl.model.Player
import com.example.paintbrawl.model.GameMode
import com.example.paintbrawl.viewmodel.GameViewModel

/**
 * Pantalla principal del juego
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit,
    onSaveGame: (() -> Unit)? = null
) {
    val gameState by viewModel.gameState.collectAsState()
    val showMatchAnimation by viewModel.showMatchAnimation.collectAsState()
    val showMismatchAnimation by viewModel.showMismatchAnimation.collectAsState()
    var showGameOverDialog by remember { mutableStateOf(false) }

    // Mostrar diálogo de fin de juego
    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver) {
            showGameOverDialog = true
        }
    }

    // Verificar si el cliente ha recibido el mazo
    val isWaitingForDeck = gameState.gameMode == GameMode.BLUETOOTH &&
            !gameState.isBluetoothHost &&
            gameState.cards.isEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(gameState.themeColor.primaryColor),
                        Color(gameState.themeColor.secondaryColor)
                    )
                )
            )
    ) {
        if (isWaitingForDeck) {
            // Pantalla de espera mientras se sincroniza el mazo
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Sincronizando mazo...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Esperando configuración del host",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                TopAppBar(
                    title = {
                        Text(
                            "MEMORAMA",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackToMenu) {
                            Icon(Icons.Default.ArrowBack, "Volver")
                        }
                    },
                    actions = {
                        // Botón de guardar solo para modos LOCAL y CLARIVIDENTE
                        if (gameState.gameMode != GameMode.BLUETOOTH && !gameState.isGameOver && onSaveGame != null) {
                            IconButton(onClick = onSaveGame) {
                                Icon(Icons.Default.Save, "Guardar")
                            }
                        }
                        IconButton(onClick = { viewModel.resetGame() }) {
                            Icon(Icons.Default.Refresh, "Reiniciar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Panel de información de jugadores
                if (gameState.gameMode != GameMode.CLARIVIDENTE) {
                    PlayerInfoPanel(
                        currentPlayer = gameState.currentPlayer,
                        score1 = gameState.player1Score,
                        score2 = gameState.player2Score,
                        isBluetoothMode = gameState.gameMode == GameMode.BLUETOOTH,
                        localPlayer = gameState.localPlayer,
                        isMyTurn = gameState.isMyTurn
                    )
                } else {
                    // Panel para modo Clarividente
                    ClarividenteInfoPanel(
                        score = gameState.player1Score,
                        lives = gameState.lives,
                        timeElapsed = gameState.timeElapsed
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Información adicional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoCard("Movimientos", gameState.movesCount.toString())
                    InfoCard("Parejas", "${gameState.pairsFound}/${gameState.cards.size / 2}")
                    if (gameState.gameMode != GameMode.CLARIVIDENTE) {
                        InfoCard("Tiempo", formatTime(gameState.timeElapsed))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Indicador de turno en modo Bluetooth
                if (gameState.gameMode == GameMode.BLUETOOTH) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (gameState.isMyTurn)
                                Color(0xFF4CAF50).copy(alpha = 0.3f)
                            else
                                Color(0xFFFF5722).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (gameState.isMyTurn) "✓ Tu turno" else "⏳ Esperando al oponente...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Grid de cartas
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(gameState.cards) { index, card ->
                        CardItem(
                            card = card,
                            onClick = { viewModel.onCardClick(index) },
                            showMatch = showMatchAnimation && gameState.selectedCards.contains(index),
                            showMismatch = showMismatchAnimation && gameState.selectedCards.contains(index),
                            isInteractionEnabled = gameState.isMyTurn && !gameState.isCheckingMatch
                        )
                    }
                }
            }

            // Diálogo de fin de juego
            if (showGameOverDialog) {
                if (gameState.gameMode == GameMode.CLARIVIDENTE) {
                    ClarividenteGameOverDialog(
                        won = gameState.lives > 0,
                        score = gameState.player1Score,
                        totalPairs = gameState.cards.size / 2,
                        moves = gameState.movesCount,
                        time = gameState.timeElapsed,
                        onDismiss = { showGameOverDialog = false },
                        onPlayAgain = {
                            viewModel.resetGame()
                            showGameOverDialog = false
                        },
                        onBackToMenu = onBackToMenu
                    )
                } else {
                    GameOverDialog(
                        winner = gameState.winner,
                        player1Score = gameState.player1Score,
                        player2Score = gameState.player2Score,
                        onDismiss = { showGameOverDialog = false },
                        onPlayAgain = {
                            viewModel.resetGame()
                            showGameOverDialog = false
                        },
                        onBackToMenu = onBackToMenu
                    )
                }
            }
        }
    }
}

/**
 * Panel de información para modo Clarividente
 */
@Composable
fun ClarividenteInfoPanel(
    score: Int,
    lives: Int,
    timeElapsed: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Puntuación
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Puntos",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = score.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Vidas
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Vidas",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    repeat(3) { index ->
                        Text(
                            text = if (index < lives) "❤️" else "🖤",
                            fontSize = 24.sp
                        )
                    }
                }
            }

            // Tiempo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Tiempo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = formatTime(timeElapsed),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Panel que muestra la información de los jugadores
 */
@Composable
fun PlayerInfoPanel(
    currentPlayer: Player,
    score1: Int,
    score2: Int,
    isBluetoothMode: Boolean,
    localPlayer: Player,
    isMyTurn: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PlayerCard(
            player = Player.PLAYER1,
            score = score1,
            isActive = currentPlayer == Player.PLAYER1,
            isLocalPlayer = !isBluetoothMode || localPlayer == Player.PLAYER1,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        PlayerCard(
            player = Player.PLAYER2,
            score = score2,
            isActive = currentPlayer == Player.PLAYER2,
            isLocalPlayer = !isBluetoothMode || localPlayer == Player.PLAYER2,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Tarjeta de información de jugador
 */
@Composable
fun PlayerCard(
    player: Player,
    score: Int,
    isActive: Boolean,
    isLocalPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Card(
        modifier = modifier
            .scale(scale)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) player.getColor().copy(alpha = 0.9f)
            else Color.White.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 12.dp else 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = player.getName(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (isLocalPlayer) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "👤",
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = score.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Tarjeta de información pequeña
 */
@Composable
fun InfoCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Componente de carta individual
 */
@Composable
fun CardItem(
    card: Card,
    onClick: () -> Unit,
    showMatch: Boolean,
    showMismatch: Boolean,
    isInteractionEnabled: Boolean = true
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    val scale by animateFloatAsState(
        targetValue = when {
            showMatch -> 1.2f
            showMismatch -> 0.9f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val alpha = if (isInteractionEnabled || card.isFlipped || card.isMatched) 1f else 0.5f

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (rotation > 90f) card.color
                else Color.White.copy(alpha = 0.3f)
            )
            .border(
                width = 3.dp,
                color = when {
                    showMatch -> Color.Green
                    showMismatch -> Color.Red
                    card.isMatched -> Color.Green.copy(alpha = 0.5f)
                    !isInteractionEnabled && !card.isFlipped -> Color.Gray.copy(alpha = 0.5f)
                    else -> Color.White.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = isInteractionEnabled && !card.isFlipped && !card.isMatched,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            // Parte trasera de la carta
            Text(
                text = "?",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.graphicsLayer { rotationY = 180f }
            )
        }
    }
}

/**
 * Diálogo de fin de juego para modo Clarividente
 */
@Composable
fun ClarividenteGameOverDialog(
    won: Boolean,
    score: Int,
    totalPairs: Int,
    moves: Int,
    time: Long,
    onDismiss: () -> Unit,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (won) "🎉" else "😔",
                    fontSize = 48.sp
                )
                Text(
                    text = if (won) "¡Victoria!" else "Game Over",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (won) Color(0xFF4CAF50) else Color(0xFFFF5722)
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (won) {
                    Text(
                        text = "¡Completaste el juego!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Te quedaste sin vidas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider()

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Parejas", fontWeight = FontWeight.Bold)
                        Text("$score/$totalPairs", fontSize = 24.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Movimientos", fontWeight = FontWeight.Bold)
                        Text(moves.toString(), fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tiempo", fontWeight = FontWeight.Bold)
                    Text(formatTime(time), fontSize = 24.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onPlayAgain) {
                Text("Jugar de nuevo")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onBackToMenu) {
                Text("Menú principal")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Diálogo de fin de juego
 */
@Composable
fun GameOverDialog(
    winner: Player?,
    player1Score: Int,
    player2Score: Int,
    onDismiss: () -> Unit,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (winner) {
                    null -> "¡Empate!"
                    else -> "¡${winner.getName()} gana!"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = winner?.getColor() ?: Color.Gray
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Puntuación Final",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Player.PLAYER1.getName(), fontWeight = FontWeight.Bold)
                        Text(player1Score.toString(), fontSize = 32.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Player.PLAYER2.getName(), fontWeight = FontWeight.Bold)
                        Text(player2Score.toString(), fontSize = 32.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onPlayAgain) {
                Text("Jugar de nuevo")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onBackToMenu) {
                Text("Menú principal")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Función para formatear tiempo
 */
private fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}