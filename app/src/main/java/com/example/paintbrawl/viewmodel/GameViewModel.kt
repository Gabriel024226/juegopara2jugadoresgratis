package com.example.paintbrawl.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paintbrawl.bluetooth.BluetoothGameManager
import com.example.paintbrawl.data.GameRepository
import com.example.paintbrawl.data.SaveGameManager
import com.example.paintbrawl.model.*
import kotlinx.coroutines.Job
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

    private val _currentTheme = MutableStateFlow(ThemeColor.AZUL)
    val currentTheme: StateFlow<ThemeColor> = _currentTheme.asStateFlow()

    private val _preferredSaveFormat = MutableStateFlow(SaveFormat.JSON)
    val preferredSaveFormat: StateFlow<SaveFormat> = _preferredSaveFormat.asStateFlow()

    private var bluetoothManager: BluetoothGameManager? = null
    private var messageCollectorJob: Job? = null
    private var connectionObserverJob: Job? = null
    private var timerJob: Job? = null

    private var pendingHostCards: List<Card>? = null
    private var isGameStartSent = false

    val saveGameManager = SaveGameManager(context)

    /**
     * Inicia una nueva partida
     */
    fun startGame(
        gameMode: GameMode,
        numberOfPairs: Int = 8,
        isHost: Boolean = false,
        theme: ThemeColor = ThemeColor.AZUL
    ) {
        isGameStartSent = false
        _currentTheme.value = theme

        if (gameMode == GameMode.BLUETOOTH) {
            startBluetoothGame(isHost, numberOfPairs)
        } else if (gameMode == GameMode.CLARIVIDENTE) {
            startClarividenteMode(theme)
        } else {
            startLocalGame(numberOfPairs, theme)
        }
    }

    private fun startClarividenteMode(theme: ThemeColor) {
        // Modo clarividente usa los 12 colores (24 cartas)
        val cards = createCardDeck(12)

        _gameState.value = GameState(
            cards = cards,
            currentPlayer = Player.PLAYER1,
            gameMode = GameMode.CLARIVIDENTE,
            player1Score = 0,
            player2Score = 0,
            movesCount = 0,
            pairsFound = 0,
            lives = 3,
            timeElapsed = 0,
            themeColor = theme,
            isMyTurn = true
        )

        startTimer()
    }

    private fun startLocalGame(numberOfPairs: Int, theme: ThemeColor) {
        val cards = createCardDeck(numberOfPairs)
        _gameState.value = GameState(
            cards = cards,
            currentPlayer = Player.PLAYER1,
            gameMode = GameMode.LOCAL,
            player1Score = 0,
            player2Score = 0,
            movesCount = 0,
            pairsFound = 0,
            timeElapsed = 0,
            themeColor = theme
        )

        startTimer()
    }

    private fun startBluetoothGame(isHost: Boolean, numberOfPairs: Int) {
        if (bluetoothManager == null) {
            bluetoothManager = BluetoothGameManager(context)
        }

        messageCollectorJob?.cancel()
        connectionObserverJob?.cancel()

        if (isHost) {
            val cards = createCardDeck(numberOfPairs)
            pendingHostCards = cards

            _gameState.value = GameState(
                cards = cards,
                currentPlayer = Player.PLAYER1,
                gameMode = GameMode.BLUETOOTH,
                player1Score = 0,
                player2Score = 0,
                movesCount = 0,
                pairsFound = 0,
                isBluetoothHost = true,
                localPlayer = Player.PLAYER1,
                isMyTurn = true,
                timeElapsed = 0
            )
        } else {
            _gameState.value = GameState(
                cards = emptyList(),
                currentPlayer = Player.PLAYER1,
                gameMode = GameMode.BLUETOOTH,
                player1Score = 0,
                player2Score = 0,
                movesCount = 0,
                pairsFound = 0,
                isBluetoothHost = false,
                localPlayer = Player.PLAYER2,
                isMyTurn = false,
                timeElapsed = 0
            )
        }

        observeBluetoothConnection(isHost)
        observeBluetoothMessages()
        startTimer()
    }

    private fun observeBluetoothConnection(isHost: Boolean) {
        connectionObserverJob = viewModelScope.launch {
            bluetoothManager?.connectionState?.collect { state ->
                _connectionState.value = state

                if (state == BluetoothGameManager.ConnectionState.CONNECTED && isHost && !isGameStartSent) {
                    delay(2000)
                    pendingHostCards?.let { cards ->
                        sendGameStart(cards)
                        isGameStartSent = true
                    }
                }
            }
        }
    }

    private fun observeBluetoothMessages() {
        messageCollectorJob = viewModelScope.launch {
            bluetoothManager?.receivedMessage?.collect { message ->
                message?.let { handleBluetoothMessage(it) }
            }
        }
    }

    /**
     * Timer para el juego
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _gameState.value
                if (!state.isGameOver && (state.gameMode != GameMode.BLUETOOTH || state.isMyTurn)) {
                    _gameState.value = state.copy(timeElapsed = state.timeElapsed + 1000)
                }
            }
        }
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

        if (state.isCheckingMatch ||
            state.selectedCards.size >= 2 ||
            state.cards[index].isFlipped ||
            state.cards[index].isMatched) {
            return
        }

        val updatedCards = state.cards.toMutableList()
        updatedCards[index] = updatedCards[index].copy(isFlipped = true)

        val updatedSelectedCards = state.selectedCards + index

        _gameState.value = state.copy(
            cards = updatedCards,
            selectedCards = updatedSelectedCards
        )

        if (state.gameMode == GameMode.BLUETOOTH) {
            sendCardFlipped(index)
        }

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
            delay(1000)

            val card1Index = state.selectedCards[0]
            val card2Index = state.selectedCards[1]
            val card1 = state.cards[card1Index]
            val card2 = state.cards[card2Index]

            val updatedCards = state.cards.toMutableList()
            val isMatch = card1.pairId == card2.pairId

            if (isMatch) {
                handleMatch(updatedCards, card1Index, card2Index, state)
            } else {
                handleMismatch(updatedCards, card1Index, card2Index, state)
            }
        }
    }

    private suspend fun handleMatch(
        updatedCards: MutableList<Card>,
        card1Index: Int,
        card2Index: Int,
        state: GameState
    ) {
        updatedCards[card1Index] = updatedCards[card1Index].copy(isMatched = true)
        updatedCards[card2Index] = updatedCards[card2Index].copy(isMatched = true)

        val newPlayer1Score = if (state.currentPlayer == Player.PLAYER1 || state.gameMode == GameMode.CLARIVIDENTE)
            state.player1Score + 1 else state.player1Score
        val newPlayer2Score = if (state.currentPlayer == Player.PLAYER2) state.player2Score + 1 else state.player2Score

        val newPairsFound = state.pairsFound + 1
        val totalPairs = state.cards.size / 2
        val isGameOver = newPairsFound == totalPairs

        _showMatchAnimation.value = true
        delay(500)
        _showMatchAnimation.value = false

        val movement = Movement(
            moveNumber = state.movementHistory.size + 1,
            player = state.currentPlayer.name,
            card1Index = card1Index,
            card2Index = card2Index,
            wasMatch = true
        )

        val newState = state.copy(
            cards = updatedCards,
            player1Score = newPlayer1Score,
            player2Score = newPlayer2Score,
            selectedCards = emptyList(),
            isCheckingMatch = false,
            pairsFound = newPairsFound,
            movesCount = state.movesCount + 1,
            isGameOver = isGameOver,
            winner = if (isGameOver) determineWinner(newPlayer1Score, newPlayer2Score, state.gameMode) else null,
            movementHistory = state.movementHistory + movement,
            isMyTurn = if (state.gameMode == GameMode.BLUETOOTH) {
                state.currentPlayer == state.localPlayer
            } else true
        )

        _gameState.value = newState

        if (state.gameMode == GameMode.BLUETOOTH) {
            sendMatchResult(card1Index, card2Index, true, state.currentPlayer, newPlayer1Score, newPlayer2Score)
        }

        if (isGameOver) {
            timerJob?.cancel()
            if (state.gameMode != GameMode.BLUETOOTH) {
                saveGameStatistics()
            }
        }
    }

    private suspend fun handleMismatch(
        updatedCards: MutableList<Card>,
        card1Index: Int,
        card2Index: Int,
        state: GameState
    ) {
        updatedCards[card1Index] = updatedCards[card1Index].copy(isFlipped = false)
        updatedCards[card2Index] = updatedCards[card2Index].copy(isFlipped = false)

        _showMismatchAnimation.value = true
        delay(500)
        _showMismatchAnimation.value = false

        val movement = Movement(
            moveNumber = state.movementHistory.size + 1,
            player = state.currentPlayer.name,
            card1Index = card1Index,
            card2Index = card2Index,
            wasMatch = false
        )

        if (state.gameMode == GameMode.CLARIVIDENTE) {
            // En modo clarividente, se pierden vidas
            val newLives = state.lives - 1
            val isGameOver = newLives <= 0

            val newState = state.copy(
                cards = updatedCards,
                selectedCards = emptyList(),
                isCheckingMatch = false,
                movesCount = state.movesCount + 1,
                lives = newLives,
                isGameOver = isGameOver,
                winner = if (isGameOver) null else state.winner,
                movementHistory = state.movementHistory + movement
            )

            _gameState.value = newState

            if (isGameOver) {
                timerJob?.cancel()
                saveGameStatistics()
            }
        } else {
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
                movementHistory = state.movementHistory + movement,
                isMyTurn = if (state.gameMode == GameMode.BLUETOOTH) {
                    nextPlayer == state.localPlayer
                } else true
            )

            _gameState.value = newState

            if (state.gameMode == GameMode.BLUETOOTH) {
                sendMatchResult(card1Index, card2Index, false, nextPlayer, state.player1Score, state.player2Score)
            }
        }
    }

    /**
     * Guarda la partida actual
     */
    suspend fun saveCurrentGame(gameName: String, tags: List<String> = emptyList()): Result<String> {
        val state = _gameState.value

        if (state.gameMode == GameMode.BLUETOOTH) {
            return Result.failure(Exception("No se pueden guardar partidas en modo Bluetooth"))
        }

        val savedCards = state.cards.map { card ->
            SavedCard(
                id = card.id,
                pairId = card.pairId,
                colorHex = String.format("#%08X", card.color.value.toInt()),
                isFlipped = card.isFlipped,
                isMatched = card.isMatched
            )
        }

        val savedGame = SavedGame(
            gameName = gameName,
            gameMode = if (state.gameMode == GameMode.CLARIVIDENTE) "CLARIVIDENTE" else "LOCAL_2PLAYERS",
            player1Name = Player.PLAYER1.getName(),
            player2Name = if (state.gameMode == GameMode.LOCAL) Player.PLAYER2.getName() else null,
            player1Score = state.player1Score,
            player2Score = if (state.gameMode == GameMode.LOCAL) state.player2Score else null,
            currentPlayer = state.currentPlayer.name,
            movesCount = state.movesCount,
            pairsFound = state.pairsFound,
            totalPairs = state.cards.size / 2,
            lives = if (state.gameMode == GameMode.CLARIVIDENTE) state.lives else null,
            timeElapsed = state.timeElapsed,
            cards = savedCards,
            selectedCards = state.selectedCards,
            isCheckingMatch = state.isCheckingMatch,
            themeColor = state.themeColor.name,
            movementHistory = state.movementHistory
        )

        return saveGameManager.saveGame(savedGame, _preferredSaveFormat.value, tags)
    }

    /**
     * Carga una partida guardada
     */
    suspend fun loadSavedGame(fileName: String): Result<Unit> {
        return try {
            val result = saveGameManager.loadGame(fileName)

            result.fold(
                onSuccess = { savedGame ->
                    timerJob?.cancel()

                    val cards = savedGame.cards.map { savedCard ->
                        Card(
                            id = savedCard.id,
                            pairId = savedCard.pairId,
                            color = Color(android.graphics.Color.parseColor(savedCard.colorHex)),
                            isFlipped = savedCard.isFlipped,
                            isMatched = savedCard.isMatched
                        )
                    }

                    val gameMode = when (savedGame.gameMode) {
                        "CLARIVIDENTE" -> GameMode.CLARIVIDENTE
                        else -> GameMode.LOCAL
                    }

                    val theme = try {
                        ThemeColor.valueOf(savedGame.themeColor)
                    } catch (e: Exception) {
                        ThemeColor.AZUL
                    }

                    _currentTheme.value = theme

                    _gameState.value = GameState(
                        cards = cards,
                        currentPlayer = try { Player.valueOf(savedGame.currentPlayer) } catch (e: Exception) { Player.PLAYER1 },
                        player1Score = savedGame.player1Score,
                        player2Score = savedGame.player2Score ?: 0,
                        selectedCards = savedGame.selectedCards,
                        isCheckingMatch = savedGame.isCheckingMatch,
                        gameMode = gameMode,
                        isGameOver = false,
                        movesCount = savedGame.movesCount,
                        pairsFound = savedGame.pairsFound,
                        lives = savedGame.lives ?: 3,
                        timeElapsed = savedGame.timeElapsed,
                        themeColor = theme,
                        movementHistory = savedGame.movementHistory
                    )

                    startTimer()

                    Result.success(Unit)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setTheme(theme: ThemeColor) {
        _currentTheme.value = theme
        val state = _gameState.value
        _gameState.value = state.copy(themeColor = theme)
    }

    fun setSaveFormat(format: SaveFormat) {
        _preferredSaveFormat.value = format
    }

    private fun handleBluetoothMessage(messageStr: String) {
        val message = BluetoothMessageSerializer.deserialize(messageStr)
        if (message == null) return

        val state = _gameState.value

        viewModelScope.launch {
            when (message) {
                is BluetoothMessage.GameStart -> {
                    val cards = message.cards.mapIndexed { index, pairId ->
                        val color = getColorForPairId(pairId)
                        Card(id = index, pairId = pairId, color = color)
                    }
                    _gameState.value = state.copy(cards = cards)
                }
                is BluetoothMessage.CardFlipped -> {
                    val updatedCards = state.cards.toMutableList()
                    updatedCards[message.cardIndex] = updatedCards[message.cardIndex].copy(isFlipped = true)
                    val updatedSelectedCards = state.selectedCards + message.cardIndex
                    _gameState.value = state.copy(
                        cards = updatedCards,
                        selectedCards = updatedSelectedCards,
                        isCheckingMatch = updatedSelectedCards.size >= 2
                    )
                    if (updatedSelectedCards.size >= 2) delay(1000)
                }
                is BluetoothMessage.MatchResult -> {
                    val updatedCards = state.cards.toMutableList()
                    if (message.isMatch) {
                        updatedCards[message.card1Index] = updatedCards[message.card1Index].copy(isFlipped = true, isMatched = true)
                        updatedCards[message.card2Index] = updatedCards[message.card2Index].copy(isFlipped = true, isMatched = true)
                        _showMatchAnimation.value = true
                        delay(500)
                        _showMatchAnimation.value = false
                    } else {
                        updatedCards[message.card1Index] = updatedCards[message.card1Index].copy(isFlipped = true)
                        updatedCards[message.card2Index] = updatedCards[message.card2Index].copy(isFlipped = true)
                        _gameState.value = state.copy(cards = updatedCards, selectedCards = listOf(message.card1Index, message.card2Index))
                        delay(300)
                        _showMismatchAnimation.value = true
                        delay(500)
                        _showMismatchAnimation.value = false
                        updatedCards[message.card1Index] = updatedCards[message.card1Index].copy(isFlipped = false)
                        updatedCards[message.card2Index] = updatedCards[message.card2Index].copy(isFlipped = false)
                    }
                    val newPairsFound = message.player1Score + message.player2Score
                    val isGameOver = newPairsFound == state.cards.size / 2
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
                        winner = if (isGameOver) determineWinner(message.player1Score, message.player2Score, state.gameMode) else null
                    )
                    if (isGameOver) timerJob?.cancel()
                }
                is BluetoothMessage.TurnChange -> {
                    _gameState.value = state.copy(
                        currentPlayer = message.newPlayer,
                        isMyTurn = message.newPlayer == state.localPlayer
                    )
                }
                BluetoothMessage.GameReset -> resetGame()
            }
        }
    }

    fun startBluetoothServer() {
        bluetoothManager?.startServer()
    }

    fun connectToDevice(device: android.bluetooth.BluetoothDevice) {
        bluetoothManager?.connectToDevice(device)
    }

    fun getPairedDevices(): Set<android.bluetooth.BluetoothDevice> {
        return bluetoothManager?.getPairedDevices() ?: emptySet()
    }

    fun disconnectBluetooth() {
        messageCollectorJob?.cancel()
        connectionObserverJob?.cancel()
        bluetoothManager?.disconnect()
        bluetoothManager = null
        pendingHostCards = null
        isGameStartSent = false
        _connectionState.value = BluetoothGameManager.ConnectionState.DISCONNECTED
    }

    private fun sendGameStart(cards: List<Card>) {
        val pairIds = cards.map { it.pairId }
        val message = BluetoothMessage.GameStart(pairIds)
        val serialized = BluetoothMessageSerializer.serialize(message)
        bluetoothManager?.sendMessage(serialized)
        viewModelScope.launch {
            delay(500)
            bluetoothManager?.sendMessage(serialized)
        }
    }

    private fun sendCardFlipped(cardIndex: Int) {
        val message = BluetoothMessage.CardFlipped(cardIndex)
        bluetoothManager?.sendMessage(BluetoothMessageSerializer.serialize(message))
    }

    private fun sendMatchResult(
        card1Index: Int,
        card2Index: Int,
        isMatch: Boolean,
        currentPlayer: Player,
        player1Score: Int,
        player2Score: Int
    ) {
        val message = BluetoothMessage.MatchResult(card1Index, card2Index, isMatch, currentPlayer, player1Score, player2Score)
        bluetoothManager?.sendMessage(BluetoothMessageSerializer.serialize(message))
    }

    private fun getColorForPairId(pairId: Int): Color {
        val colors = listOf(
            Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5), Color(0xFF2196F3),
            Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50), Color(0xFFFF9800),
            Color(0xFFFF5722), Color(0xFF795548), Color(0xFFFFEB3B), Color(0xFFF44336)
        )
        return colors[pairId % colors.size]
    }

    private fun determineWinner(score1: Int, score2: Int, gameMode: GameMode): Player? {
        if (gameMode == GameMode.CLARIVIDENTE) {
            return Player.PLAYER1
        }
        return when {
            score1 > score2 -> Player.PLAYER1
            score2 > score1 -> Player.PLAYER2
            else -> null
        }
    }

    fun resetGame() {
        val currentMode = _gameState.value.gameMode
        val numberOfPairs = _gameState.value.cards.size / 2
        val isHost = _gameState.value.isBluetoothHost
        val theme = _gameState.value.themeColor

        if (currentMode == GameMode.BLUETOOTH && isHost) {
            bluetoothManager?.sendMessage(BluetoothMessageSerializer.serialize(BluetoothMessage.GameReset))
        }

        timerJob?.cancel()
        startGame(currentMode, numberOfPairs, isHost, theme)
    }

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

            if (state.gameMode == GameMode.LOCAL) {
                repository.updatePlayerStats(
                    playerName = player2Name,
                    won = state.winner == Player.PLAYER2,
                    pairsFound = state.player2Score,
                    moves = state.movesCount,
                    score = state.player2Score
                )
            }

            repository.saveGameHistory(
                player1Name = player1Name,
                player2Name = if (state.gameMode == GameMode.LOCAL) player2Name else "Clarividente",
                player1Score = state.player1Score,
                player2Score = if (state.gameMode == GameMode.LOCAL) state.player2Score else 0,
                winner = state.winner?.getName(),
                totalMoves = state.movesCount,
                gameMode = state.gameMode.name
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        disconnectBluetooth()
    }
}

class GameViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(GameRepository(context), context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}