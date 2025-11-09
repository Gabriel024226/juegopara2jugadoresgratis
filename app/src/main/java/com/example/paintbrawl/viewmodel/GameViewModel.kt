package com.example.paintbrawl.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
class GameViewModel(private val repository: GameRepository) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _showMatchAnimation = MutableStateFlow(false)
    val showMatchAnimation: StateFlow<Boolean> = _showMatchAnimation.asStateFlow()

    private val _showMismatchAnimation = MutableStateFlow(false)
    val showMismatchAnimation: StateFlow<Boolean> = _showMismatchAnimation.asStateFlow()

    /**
     * Inicia una nueva partida
     */
    fun startGame(gameMode: GameMode, numberOfPairs: Int = 8, isHost: Boolean = false) {
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

    /**
     * Maneja el clic en una carta
     */
    fun onCardClick(index: Int) {
        val state = _gameState.value

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

                val newScore = when (state.currentPlayer) {
                    Player.PLAYER1 -> state.player1Score + 1
                    Player.PLAYER2 -> state.player2Score + 1
                }

                val newPairsFound = state.pairsFound + 1
                val totalPairs = state.cards.size / 2
                val isGameOver = newPairsFound == totalPairs

                // Mostrar animación de éxito
                _showMatchAnimation.value = true
                delay(500)
                _showMatchAnimation.value = false

                _gameState.value = state.copy(
                    cards = updatedCards,
                    player1Score = if (state.currentPlayer == Player.PLAYER1) newScore else state.player1Score,
                    player2Score = if (state.currentPlayer == Player.PLAYER2) newScore else state.player2Score,
                    selectedCards = emptyList(),
                    isCheckingMatch = false,
                    pairsFound = newPairsFound,
                    movesCount = state.movesCount + 1,
                    isGameOver = isGameOver,
                    winner = if (isGameOver) determineWinner(
                        if (state.currentPlayer == Player.PLAYER1) newScore else state.player1Score,
                        if (state.currentPlayer == Player.PLAYER2) newScore else state.player2Score
                    ) else null
                )

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

                _gameState.value = state.copy(
                    cards = updatedCards,
                    selectedCards = emptyList(),
                    isCheckingMatch = false,
                    currentPlayer = nextPlayer,
                    movesCount = state.movesCount + 1
                )
            }
        }
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
        startGame(currentMode, numberOfPairs)
    }

    /**
     * Guarda las estadísticas del juego en la base de datos
     */
    private fun saveGameStatistics() {
        viewModelScope.launch {
            val state = _gameState.value
            val player1Name = Player.PLAYER1.getName()
            val player2Name = Player.PLAYER2.getName()

            // Actualizar estadísticas de ambos jugadores
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

            // Guardar historial del juego
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
     * Obtiene las estadísticas de un jugador
     */
    fun getPlayerStats(playerName: String) = viewModelScope.launch {
        repository.getPlayerStats(playerName)
    }
}

/**
 * Factory para crear el ViewModel con el repositorio
 */
class GameViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(GameRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}