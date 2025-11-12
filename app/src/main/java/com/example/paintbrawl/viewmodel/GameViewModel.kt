package com.example.paintbrawl.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paintbrawl.bluetooth.BluetoothGameManager
import com.example.paintbrawl.data.GameRepository
import com.example.paintbrawl.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel que maneja la lógica del juego de memorama
 */
class GameViewModel(
    private val repository: GameRepository,
    private val context: Context
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _showMatchAnimation = MutableStateFlow(false)
    val showMatchAnimation: StateFlow<Boolean> = _showMatchAnimation.asStateFlow()

    private val _showMismatchAnimation = MutableStateFlow(false)
    val showMismatchAnimation: StateFlow<Boolean> = _showMismatchAnimation.asStateFlow()

    private val _connectionState = MutableStateFlow<BluetoothGameManager.ConnectionState>(
        BluetoothGameManager.ConnectionState.DISCONNECTED
    )
    val connectionState: StateFlow<BluetoothGameManager.ConnectionState> = _connectionState.asStateFlow()

    private var bluetoothManager: BluetoothGameManager? = null
    private var messageCollectorJob: kotlinx.coroutines.Job? = null
    private var connectionObserverJob: kotlinx.coroutines.Job? = null

    // Guardar el mazo del host para enviarlo cuando se conecte
    private var pendingHostCards: List<Card>? = null
    private var isGameStartSent = false

    /**
     * Inicia una nueva partida
     */
    fun startGame(gameMode: GameMode, numberOfPairs: Int = 8, isHost: Boolean = false) {
        // Limpiar estado anterior
        isGameStartSent = false

        if (gameMode == GameMode.BLUETOOTH) {
            // Inicializar Bluetooth solo si no existe
            if (bluetoothManager == null) {
                bluetoothManager = BluetoothGameManager(context)
                android.util.Log.d("GameViewModel", "BluetoothManager creado")
            }

            // Cancelar jobs anteriores
            messageCollectorJob?.cancel()
            connectionObserverJob?.cancel()

            if (isHost) {
                // El HOST genera el mazo
                val cards = createCardDeck(numberOfPairs)
                pendingHostCards = cards

                android.util.Log.d("GameViewModel", "HOST: Mazo generado")
                android.util.Log.d("GameViewModel", "HOST: Cartas en orden:")
                cards.forEachIndexed { index, card ->
                    android.util.Log.d("GameViewModel", "  [$index] -> pairId=${card.pairId}, color=${card.color}")
                }

                _gameState.value = GameState(
                    cards = cards,
                    currentPlayer = Player.PLAYER1,
                    gameMode = gameMode,
                    player1Score = 0,
                    player2Score = 0,
                    movesCount = 0,
                    pairsFound = 0,
                    isBluetoothHost = true,
                    localPlayer = Player.PLAYER1,
                    isMyTurn = true
                )
            } else {
                // El CLIENTE espera recibir el mazo
                android.util.Log.d("GameViewModel", "CLIENTE: Esperando mazo del host")

                _gameState.value = GameState(
                    cards = emptyList(), // Vacío hasta recibir GameStart
                    currentPlayer = Player.PLAYER1,
                    gameMode = gameMode,
                    player1Score = 0,
                    player2Score = 0,
                    movesCount = 0,
                    pairsFound = 0,
                    isBluetoothHost = false,
                    localPlayer = Player.PLAYER2,
                    isMyTurn = false
                )
            }

            // Observar estado de conexión
            connectionObserverJob = viewModelScope.launch {
                bluetoothManager?.connectionState?.collect { state ->
                    android.util.Log.d("GameViewModel", "Estado conexión: $state")
                    _connectionState.value = state

                    // Cuando se establece conexión, el host envía el juego inicial
                    if (state == BluetoothGameManager.ConnectionState.CONNECTED && isHost && !isGameStartSent) {
                        delay(2000) // Espera mayor para asegurar que la conexión está completamente lista
                        android.util.Log.d("GameViewModel", "HOST: Conexión establecida, enviando GameStart")
                        pendingHostCards?.let { cards ->
                            sendGameStart(cards)
                            isGameStartSent = true
                        }
                    }

                    // Si la conexión se pierde, resetear
                    if (state == BluetoothGameManager.ConnectionState.DISCONNECTED) {
                        android.util.Log.d("GameViewModel", "Conexión perdida, limpiando recursos")
                    }
                }
            }

            // Observar mensajes recibidos
            messageCollectorJob = viewModelScope.launch {
                bluetoothManager?.receivedMessage?.collect { message ->
                    message?.let {
                        android.util.Log.d("GameViewModel", "=== MENSAJE RECIBIDO ===")
                        android.util.Log.d("GameViewModel", "Raw: $it")
                        handleBluetoothMessage(it)
                    }
                }
            }
        } else {
            // Modo LOCAL
            val cards = createCardDeck(numberOfPairs)
            _gameState.value = GameState(
                cards = cards,
                currentPlayer = Player.PLAYER1,
                gameMode = gameMode,
                player1Score = 0,
                player2Score = 0,
                movesCount = 0,
                pairsFound = 0
            )
        }
    }

    /**
     * Inicia el servidor Bluetooth (para el host)
     */
    fun startBluetoothServer() {
        bluetoothManager?.startServer()
    }

    /**
     * Conecta a un dispositivo Bluetooth (para el cliente)
     */
    fun connectToDevice(device: android.bluetooth.BluetoothDevice) {
        bluetoothManager?.connectToDevice(device)
    }

    /**
     * Obtiene dispositivos emparejados
     */
    fun getPairedDevices(): Set<android.bluetooth.BluetoothDevice> {
        return bluetoothManager?.getPairedDevices() ?: emptySet()
    }

    /**
     * Desconectar Bluetooth y limpiar recursos
     */
    fun disconnectBluetooth() {
        android.util.Log.d("GameViewModel", "Desconectando Bluetooth manualmente")
        messageCollectorJob?.cancel()
        connectionObserverJob?.cancel()
        bluetoothManager?.disconnect()
        bluetoothManager = null
        pendingHostCards = null
        isGameStartSent = false
        _connectionState.value = BluetoothGameManager.ConnectionState.DISCONNECTED
    }

    /**
     * Maneja el clic en una carta
     */
    fun onCardClick(index: Int) {
        val state = _gameState.value

        // En modo Bluetooth, solo permitir clic si es el turno del jugador local
        if (state.gameMode == GameMode.BLUETOOTH && !state.isMyTurn) {
            return
        }

        // Validaciones: no permitir clic si ya hay 2 cartas volteadas o si está verificando
        if (state.isCheckingMatch ||
            state.selectedCards.size >= 2 ||
            state.cards[index].isFlipped ||
            state.cards[index].isMatched) {
            return
        }

        // Voltear la carta
        val updatedCards = state.cards.toMutableList()
        updatedCards[index] = updatedCards[index].copy(isFlipped = true)

        val updatedSelectedCards = state.selectedCards + index

        _gameState.value = state.copy(
            cards = updatedCards,
            selectedCards = updatedSelectedCards
        )

        // Enviar mensaje por Bluetooth
        if (state.gameMode == GameMode.BLUETOOTH) {
            sendCardFlipped(index)
        }

        // Si se seleccionaron 2 cartas, verificar si son pareja
        if (updatedSelectedCards.size == 2) {
            checkForMatch()
        }
    }

    /**
     * Verifica si las dos cartas seleccionadas forman un par
     */
    private fun checkForMatch() {
        val state = _gameState.value
        _gameState.value = state.copy(isCheckingMatch = true)

        viewModelScope.launch {
            delay(1000) // Dar tiempo para que el jugador vea ambas cartas

            val card1Index = state.selectedCards[0]
            val card2Index = state.selectedCards[1]
            val card1 = state.cards[card1Index]
            val card2 = state.cards[card2Index]

            val updatedCards = state.cards.toMutableList()
            val isMatch = card1.pairId == card2.pairId

            if (isMatch) {
                // Las cartas coinciden
                updatedCards[card1Index] = card1.copy(isMatched = true)
                updatedCards[card2Index] = card2.copy(isMatched = true)

                val newPlayer1Score = if (state.currentPlayer == Player.PLAYER1) state.player1Score + 1 else state.player1Score
                val newPlayer2Score = if (state.currentPlayer == Player.PLAYER2) state.player2Score + 1 else state.player2Score

                val newPairsFound = state.pairsFound + 1
                val totalPairs = state.cards.size / 2
                val isGameOver = newPairsFound == totalPairs

                // Mostrar animación de éxito
                _showMatchAnimation.value = true
                delay(500)
                _showMatchAnimation.value = false

                val newState = state.copy(
                    cards = updatedCards,
                    player1Score = newPlayer1Score,
                    player2Score = newPlayer2Score,
                    selectedCards = emptyList(),
                    isCheckingMatch = false,
                    pairsFound = newPairsFound,
                    movesCount = state.movesCount + 1,
                    isGameOver = isGameOver,
                    winner = if (isGameOver) determineWinner(newPlayer1Score, newPlayer2Score) else null,
                    // El jugador mantiene su turno si acertó
                    isMyTurn = if (state.gameMode == GameMode.BLUETOOTH) {
                        state.currentPlayer == state.localPlayer
                    } else true
                )

                _gameState.value = newState

                // Enviar resultado por Bluetooth (el jugador mantiene turno)
                if (state.gameMode == GameMode.BLUETOOTH) {
                    sendMatchResult(
                        card1Index, card2Index, true,
                        state.currentPlayer, newPlayer1Score, newPlayer2Score
                    )
                }

                // Si el juego terminó, guardar estadísticas
                if (isGameOver) {
                    saveGameStatistics()
                }
            } else {
                // Las cartas no coinciden
                updatedCards[card1Index] = card1.copy(isFlipped = false)
                updatedCards[card2Index] = card2.copy(isFlipped = false)

                // Mostrar animación de error
                _showMismatchAnimation.value = true
                delay(500)
                _showMismatchAnimation.value = false

                // Cambiar de jugador
                val nextPlayer = when (state.currentPlayer) {
                    Player.PLAYER1 -> Player.PLAYER2
                    Player.PLAYER2 -> Player.PLAYER1
                }

                val newState = state.copy(
                    cards = updatedCards,
                    selectedCards = emptyList(),
                    isCheckingMatch = false,
                    currentPlayer = nextPlayer,
                    movesCount = state.movesCount + 1,
                    isMyTurn = if (state.gameMode == GameMode.BLUETOOTH) {
                        nextPlayer == state.localPlayer
                    } else true
                )

                _gameState.value = newState

                // Enviar resultado por Bluetooth (cambio de turno)
                if (state.gameMode == GameMode.BLUETOOTH) {
                    sendMatchResult(
                        card1Index, card2Index, false,
                        nextPlayer, state.player1Score, state.player2Score
                    )
                }
            }
        }
    }

    /**
     * Maneja mensajes recibidos por Bluetooth
     */
    private fun handleBluetoothMessage(messageStr: String) {
        val message = BluetoothMessageSerializer.deserialize(messageStr)

        if (message == null) {
            android.util.Log.e("GameViewModel", "ERROR: No se pudo deserializar el mensaje: $messageStr")
            return
        }

        val state = _gameState.value

        viewModelScope.launch {
            when (message) {
                is BluetoothMessage.GameStart -> {
                    android.util.Log.d("GameViewModel", "CLIENTE: ===== RECIBIENDO GAME START =====")
                    android.util.Log.d("GameViewModel", "CLIENTE: Número de pairIds: ${message.cards.size}")
                    android.util.Log.d("GameViewModel", "CLIENTE: PairIds = ${message.cards}")

                    // Crear las cartas EN EL MISMO ORDEN que el host
                    val cards = message.cards.mapIndexed { index, pairId ->
                        val color = getColorForPairId(pairId)
                        Card(id = index, pairId = pairId, color = color)
                    }

                    android.util.Log.d("GameViewModel", "CLIENTE: Cartas reconstruidas:")
                    cards.forEachIndexed { index, card ->
                        android.util.Log.d("GameViewModel", "  [$index] -> pairId=${card.pairId}, color=${card.color}")
                    }

                    // Actualizar el estado con el mazo recibido
                    _gameState.value = state.copy(cards = cards)
                    android.util.Log.d("GameViewModel", "CLIENTE: Mazo sincronizado correctamente")
                }

                is BluetoothMessage.CardFlipped -> {
                    android.util.Log.d("GameViewModel", "Carta volteada por oponente: ${message.cardIndex}")

                    // Voltear la carta del oponente
                    val updatedCards = state.cards.toMutableList()
                    updatedCards[message.cardIndex] = updatedCards[message.cardIndex].copy(isFlipped = true)

                    val updatedSelectedCards = state.selectedCards + message.cardIndex

                    _gameState.value = state.copy(
                        cards = updatedCards,
                        selectedCards = updatedSelectedCards,
                        isCheckingMatch = updatedSelectedCards.size >= 2
                    )

                    // Si ya hay 2 cartas, esperar el resultado del match
                    if (updatedSelectedCards.size >= 2) {
                        delay(1000)
                    }
                }

                is BluetoothMessage.MatchResult -> {
                    android.util.Log.d("GameViewModel", "Resultado del match: isMatch=${message.isMatch}")

                    // Aplicar resultado del match
                    val updatedCards = state.cards.toMutableList()

                    if (message.isMatch) {
                        updatedCards[message.card1Index] = updatedCards[message.card1Index].copy(
                            isFlipped = true,
                            isMatched = true
                        )
                        updatedCards[message.card2Index] = updatedCards[message.card2Index].copy(
                            isFlipped = true,
                            isMatched = true
                        )

                        _showMatchAnimation.value = true
                        delay(500)
                        _showMatchAnimation.value = false
                    } else {
                        // Primero asegurarse de que las cartas estén volteadas
                        updatedCards[message.card1Index] = updatedCards[message.card1Index].copy(isFlipped = true)
                        updatedCards[message.card2Index] = updatedCards[message.card2Index].copy(isFlipped = true)

                        // Actualizar temporalmente para mostrar las cartas
                        _gameState.value = state.copy(
                            cards = updatedCards,
                            selectedCards = listOf(message.card1Index, message.card2Index)
                        )

                        delay(300)

                        _showMismatchAnimation.value = true
                        delay(500)
                        _showMismatchAnimation.value = false

                        // Voltear de vuelta
                        updatedCards[message.card1Index] = updatedCards[message.card1Index].copy(isFlipped = false)
                        updatedCards[message.card2Index] = updatedCards[message.card2Index].copy(isFlipped = false)
                    }

                    val newPairsFound = message.player1Score + message.player2Score
                    val totalPairs = state.cards.size / 2
                    val isGameOver = newPairsFound == totalPairs

                    _gameState.value = state.copy(
                        cards = updatedCards,
                        selectedCards = emptyList(),
                        isCheckingMatch = false,
                        currentPlayer = message.currentPlayer,
                        player1Score = message.player1Score,
                        player2Score = message.player2Score,
                        pairsFound = newPairsFound,
                        movesCount = state.movesCount + 1,
                        isMyTurn = message.currentPlayer == state.localPlayer,
                        isGameOver = isGameOver,
                        winner = if (isGameOver) determineWinner(message.player1Score, message.player2Score) else null
                    )

                    if (isGameOver) {
                        saveGameStatistics()
                    }
                }

                is BluetoothMessage.TurnChange -> {
                    _gameState.value = state.copy(
                        currentPlayer = message.newPlayer,
                        isMyTurn = message.newPlayer == state.localPlayer
                    )
                }

                BluetoothMessage.GameReset -> {
                    android.util.Log.d("GameViewModel", "Reinicio solicitado por oponente")
                    resetGame()
                }
            }
        }
    }

    /**
     * Envía el inicio del juego por Bluetooth (solo host)
     */
    private fun sendGameStart(cards: List<Card>) {
        // Enviar los pairIds en el orden exacto
        val pairIds = cards.map { it.pairId }
        val message = BluetoothMessage.GameStart(pairIds)
        val serialized = BluetoothMessageSerializer.serialize(message)

        android.util.Log.d("GameViewModel", "HOST: ===== ENVIANDO GAME START =====")
        android.util.Log.d("GameViewModel", "HOST: Número de cartas: ${pairIds.size}")
        android.util.Log.d("GameViewModel", "HOST: PairIds = $pairIds")
        android.util.Log.d("GameViewModel", "HOST: Mensaje serializado: $serialized")

        bluetoothManager?.sendMessage(serialized)

        // Enviar dos veces para asegurar que llega
        viewModelScope.launch {
            delay(500)
            android.util.Log.d("GameViewModel", "HOST: Reenviando GameStart por seguridad")
            bluetoothManager?.sendMessage(serialized)
        }
    }

    /**
     * Envía que se volteó una carta
     */
    private fun sendCardFlipped(cardIndex: Int) {
        val message = BluetoothMessage.CardFlipped(cardIndex)
        val serialized = BluetoothMessageSerializer.serialize(message)
        android.util.Log.d("GameViewModel", "Enviando CardFlipped: $serialized")
        bluetoothManager?.sendMessage(serialized)
    }

    /**
     * Envía el resultado de un intento de match
     */
    private fun sendMatchResult(
        card1Index: Int,
        card2Index: Int,
        isMatch: Boolean,
        currentPlayer: Player,
        player1Score: Int,
        player2Score: Int
    ) {
        val message = BluetoothMessage.MatchResult(
            card1Index, card2Index, isMatch, currentPlayer, player1Score, player2Score
        )
        val serialized = BluetoothMessageSerializer.serialize(message)
        android.util.Log.d("GameViewModel", "Enviando MatchResult: $serialized")
        bluetoothManager?.sendMessage(serialized)
    }

    /**
     * Obtiene el color para un pairId de forma consistente
     */
    private fun getColorForPairId(pairId: Int): androidx.compose.ui.graphics.Color {
        val colors = listOf(
            androidx.compose.ui.graphics.Color(0xFFE91E63), // Pink
            androidx.compose.ui.graphics.Color(0xFF9C27B0), // Purple
            androidx.compose.ui.graphics.Color(0xFF3F51B5), // Indigo
            androidx.compose.ui.graphics.Color(0xFF2196F3), // Blue
            androidx.compose.ui.graphics.Color(0xFF00BCD4), // Cyan
            androidx.compose.ui.graphics.Color(0xFF009688), // Teal
            androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green
            androidx.compose.ui.graphics.Color(0xFFFF9800), // Orange
            androidx.compose.ui.graphics.Color(0xFFFF5722), // Deep Orange
            androidx.compose.ui.graphics.Color(0xFF795548), // Brown
            androidx.compose.ui.graphics.Color(0xFFFFEB3B), // Yellow
            androidx.compose.ui.graphics.Color(0xFFF44336)  // Red
        )
        return colors[pairId % colors.size]
    }

    /**
     * Determina el ganador del juego
     */
    private fun determineWinner(score1: Int, score2: Int): Player? {
        return when {
            score1 > score2 -> Player.PLAYER1
            score2 > score1 -> Player.PLAYER2
            else -> null // Empate
        }
    }

    /**
     * Reinicia el juego con las mismas configuraciones
     */
    fun resetGame() {
        val currentMode = _gameState.value.gameMode
        val numberOfPairs = _gameState.value.cards.size / 2
        val isHost = _gameState.value.isBluetoothHost

        if (currentMode == GameMode.BLUETOOTH && isHost) {
            bluetoothManager?.sendMessage(BluetoothMessageSerializer.serialize(BluetoothMessage.GameReset))
        }

        startGame(currentMode, numberOfPairs, isHost)
    }

    /**
     * Guarda las estadísticas del juego en la base de datos
     */
    private fun saveGameStatistics() {
        viewModelScope.launch {
            val state = _gameState.value
            val player1Name = Player.PLAYER1.getName()
            val player2Name = Player.PLAYER2.getName()

            repository.updatePlayerStats(
                playerName = player1Name,
                won = state.winner == Player.PLAYER1,
                pairsFound = state.player1Score,
                moves = state.movesCount,
                score = state.player1Score
            )

            repository.updatePlayerStats(
                playerName = player2Name,
                won = state.winner == Player.PLAYER2,
                pairsFound = state.player2Score,
                moves = state.movesCount,
                score = state.player2Score
            )

            repository.saveGameHistory(
                player1Name = player1Name,
                player2Name = player2Name,
                player1Score = state.player1Score,
                player2Score = state.player2Score,
                winner = state.winner?.getName(),
                totalMoves = state.movesCount,
                gameMode = state.gameMode.name
            )
        }
    }

    /**
     * Limpia recursos cuando el ViewModel se destruye
     */
    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("GameViewModel", "onCleared - limpiando todos los recursos")
        disconnectBluetooth()
    }
}

/**
 * Factory para crear el ViewModel con el repositorio
 */
class GameViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(GameRepository(context), context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}