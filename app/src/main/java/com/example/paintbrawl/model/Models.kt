package com.example.paintbrawl.model

import androidx.compose.ui.graphics.Color
import com.example.paintbrawl.ui.theme.*

/**
 * Enumeración que representa los diferentes modos de juego
 */
enum class GameMode {
    LOCAL,      // Dos jugadores en el mismo dispositivo
    BLUETOOTH   // Dos jugadores vía Bluetooth
}

/**
 * Enumeración que representa a los jugadores
 */
enum class Player {
    PLAYER1,
    PLAYER2;

    fun getColor(): Color {
        return when (this) {
            PLAYER1 -> Color(0xFF6650a4)
            PLAYER2 -> Color(0xFF03DAC5)
        }
    }

    fun getName(): String {
        return when (this) {
            PLAYER1 -> "Jugador 1"
            PLAYER2 -> "Jugador 2"
        }
    }
}

/**
 * Data class que representa una carta del memorama
 */
data class Card(
    val id: Int,
    val pairId: Int,      // ID del par (dos cartas con el mismo pairId forman un par)
    val color: Color,
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

/**
 * Data class que representa el estado del juego
 */
data class GameState(
    val cards: List<Card> = emptyList(),
    val currentPlayer: Player = Player.PLAYER1,
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val selectedCards: List<Int> = emptyList(),  // Índices de cartas seleccionadas
    val isCheckingMatch: Boolean = false,
    val gameMode: GameMode = GameMode.LOCAL,
    val isGameOver: Boolean = false,
    val winner: Player? = null,
    val movesCount: Int = 0,
    val pairsFound: Int = 0,
    val isBluetoothHost: Boolean = false,  // Si es el host en modo Bluetooth
    val localPlayer: Player = Player.PLAYER1,  // Qué jugador es este dispositivo
    val isMyTurn: Boolean = true  // Si es el turno del jugador local
)

/**
 * Sealed class para representar el resultado de un juego
 */
sealed class GameResult {
    data class Win(val winner: Player, val score: Int) : GameResult()
    data class Tie(val score: Int) : GameResult()
}

/**
 * Data class para mensajes de Bluetooth
 */
sealed class BluetoothMessage {
    data class GameStart(val cards: List<Int>) : BluetoothMessage() // Lista de pairIds en orden
    data class CardFlipped(val cardIndex: Int) : BluetoothMessage()
    data class MatchResult(
        val card1Index: Int,
        val card2Index: Int,
        val isMatch: Boolean,
        val currentPlayer: Player,
        val player1Score: Int,
        val player2Score: Int
    ) : BluetoothMessage()
    data class TurnChange(val newPlayer: Player) : BluetoothMessage()
    object GameReset : BluetoothMessage()
}

/**
 * Clase para serializar/deserializar mensajes Bluetooth
 */
object BluetoothMessageSerializer {
    fun serialize(message: BluetoothMessage): String {
        return when (message) {
            is BluetoothMessage.GameStart -> {
                "GAME_START|${message.cards.joinToString(",")}"
            }
            is BluetoothMessage.CardFlipped -> {
                "CARD_FLIPPED|${message.cardIndex}"
            }
            is BluetoothMessage.MatchResult -> {
                "MATCH_RESULT|${message.card1Index}|${message.card2Index}|${message.isMatch}|${message.currentPlayer.name}|${message.player1Score}|${message.player2Score}"
            }
            is BluetoothMessage.TurnChange -> {
                "TURN_CHANGE|${message.newPlayer.name}"
            }
            BluetoothMessage.GameReset -> "GAME_RESET"
        }
    }

    fun deserialize(data: String): BluetoothMessage? {
        val parts = data.split("|")
        return try {
            when (parts[0]) {
                "GAME_START" -> {
                    val cards = parts[1].split(",").map { it.toInt() }
                    BluetoothMessage.GameStart(cards)
                }
                "CARD_FLIPPED" -> {
                    BluetoothMessage.CardFlipped(parts[1].toInt())
                }
                "MATCH_RESULT" -> {
                    BluetoothMessage.MatchResult(
                        card1Index = parts[1].toInt(),
                        card2Index = parts[2].toInt(),
                        isMatch = parts[3].toBoolean(),
                        currentPlayer = Player.valueOf(parts[4]),
                        player1Score = parts[5].toInt(),
                        player2Score = parts[6].toInt()
                    )
                }
                "TURN_CHANGE" -> {
                    BluetoothMessage.TurnChange(Player.valueOf(parts[1]))
                }
                "GAME_RESET" -> BluetoothMessage.GameReset
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Función para crear el mazo de cartas del memorama
 */
fun createCardDeck(numberOfPairs: Int = 8): List<Card> {
    val colors = listOf(
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF3F51B5), // Indigo
        Color(0xFF2196F3), // Blue
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548), // Brown
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFF44336)  // Red
    )

    val cards = mutableListOf<Card>()
    var cardId = 0

    // Crear pares de cartas
    for (i in 0 until numberOfPairs) {
        val color = colors[i % colors.size]
        cards.add(Card(cardId++, i, color))
        cards.add(Card(cardId++, i, color))
    }

    // Mezclar las cartas
    return cards.shuffled()
}