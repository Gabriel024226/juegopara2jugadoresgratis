package com.example.paintbrawl.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enumeración para los formatos de guardado soportados
 */
enum class SaveFormat {
    TXT, XML, JSON
}

/**
 * Data class que representa una partida guardada
 */

data class SavedGame(
    val id: String = UUID.randomUUID().toString(),
    val gameName: String,
    val gameMode: String, // "LOCAL_2PLAYERS" o "CLARIVIDENTE"
    val timestamp: Long = System.currentTimeMillis(),
    val player1Name: String,
    val player2Name: String? = null, // null para modo clarividente
    val player1Score: Int,
    val player2Score: Int? = null,
    val currentPlayer: String,
    val movesCount: Int,
    val pairsFound: Int,
    val totalPairs: Int,
    val lives: Int? = null, // Solo para modo clarividente
    val timeElapsed: Long, // En milisegundos
    val cards: List<SavedCard>,
    val selectedCards: List<Int>,
    val isCheckingMatch: Boolean,
    val themeColor: String, // "GUINDA" o "AZUL"
    val movementHistory: List<Movement> = emptyList()
) {
    /**
     * Convierte el objeto a formato TXT
     */
    fun toTxtFormat(): String {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val builder = StringBuilder()

            builder.appendLine("=== PARTIDA GUARDADA ===")
            builder.appendLine("ID: $id")
            builder.appendLine("Nombre: $gameName")
            builder.appendLine("Modo de juego: $gameMode")
            builder.appendLine("Fecha: ${dateFormat.format(Date(timestamp))}")
            builder.appendLine("Tema: $themeColor")
            builder.appendLine()

            builder.appendLine("=== JUGADORES ===")
            builder.appendLine("Jugador 1: $player1Name - Puntuación: $player1Score")
            if (player2Name != null && player2Score != null) {
                builder.appendLine("Jugador 2: $player2Name - Puntuación: $player2Score")
            }
            builder.appendLine("Turno actual: $currentPlayer")
            builder.appendLine()

            builder.appendLine("=== ESTADO DEL JUEGO ===")
            builder.appendLine("Movimientos realizados: $movesCount")
            builder.appendLine("Parejas encontradas: $pairsFound de $totalPairs")
            if (lives != null) {
                builder.appendLine("Vidas restantes: $lives")
            }
            builder.appendLine("Tiempo transcurrido: ${formatTime(timeElapsed)}")
            builder.appendLine("Verificando coincidencia: ${if (isCheckingMatch) "Sí" else "No"}")
            builder.appendLine()

            builder.appendLine("=== CARTAS ===")
            cards.forEachIndexed { index, card ->
                builder.appendLine("Carta $index: pairId=${card.pairId}, color=${card.colorHex}, volteada=${card.isFlipped}, emparejada=${card.isMatched}")
            }
            builder.appendLine()

            if (selectedCards.isNotEmpty()) {
                builder.appendLine("=== CARTAS SELECCIONADAS ===")
                builder.appendLine(selectedCards.joinToString(", "))
                builder.appendLine()
            }

            if (movementHistory.isNotEmpty()) {
                builder.appendLine("=== HISTORIAL DE MOVIMIENTOS ===")
                movementHistory.forEach { movement ->
                    builder.appendLine("${movement.moveNumber}. ${movement.player} - Cartas: ${movement.card1Index}, ${movement.card2Index} - ${if (movement.wasMatch) "¡Match!" else "Fallo"}")
                }
            }

            builder.toString()
        } catch (e: Exception) {
            android.util.Log.e("SavedGame", "Error en toTxtFormat", e)
            "Error al generar formato TXT: ${e.message}"
        }
    }

    /**
     * Convierte el objeto a formato XML
     */
    fun toXmlFormat(): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val builder = StringBuilder()

            builder.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            builder.appendLine("<SavedGame>")
            builder.appendLine("  <id>$id</id>")
            builder.appendLine("  <gameName><![CDATA[$gameName]]></gameName>")
            builder.appendLine("  <gameMode>$gameMode</gameMode>")
            builder.appendLine("  <timestamp>${dateFormat.format(Date(timestamp))}</timestamp>")
            builder.appendLine("  <themeColor>$themeColor</themeColor>")
            builder.appendLine()

            builder.appendLine("  <players>")
            builder.appendLine("    <player1>")
            builder.appendLine("      <name><![CDATA[$player1Name]]></name>")
            builder.appendLine("      <score>$player1Score</score>")
            builder.appendLine("    </player1>")
            if (player2Name != null && player2Score != null) {
                builder.appendLine("    <player2>")
                builder.appendLine("      <name><![CDATA[$player2Name]]></name>")
                builder.appendLine("      <score>$player2Score</score>")
                builder.appendLine("    </player2>")
            }
            builder.appendLine("    <currentPlayer>$currentPlayer</currentPlayer>")
            builder.appendLine("  </players>")
            builder.appendLine()

            builder.appendLine("  <gameState>")
            builder.appendLine("    <movesCount>$movesCount</movesCount>")
            builder.appendLine("    <pairsFound>$pairsFound</pairsFound>")
            builder.appendLine("    <totalPairs>$totalPairs</totalPairs>")
            if (lives != null) {
                builder.appendLine("    <lives>$lives</lives>")
            }
            builder.appendLine("    <timeElapsed>$timeElapsed</timeElapsed>")
            builder.appendLine("    <isCheckingMatch>$isCheckingMatch</isCheckingMatch>")
            builder.appendLine("  </gameState>")
            builder.appendLine()

            builder.appendLine("  <cards>")
            cards.forEach { card ->
                builder.appendLine("    <card>")
                builder.appendLine("      <id>${card.id}</id>")
                builder.appendLine("      <pairId>${card.pairId}</pairId>")
                builder.appendLine("      <colorHex><![CDATA[${card.colorHex}]]></colorHex>")
                builder.appendLine("      <isFlipped>${card.isFlipped}</isFlipped>")
                builder.appendLine("      <isMatched>${card.isMatched}</isMatched>")
                builder.appendLine("    </card>")
            }
            builder.appendLine("  </cards>")
            builder.appendLine()

            if (selectedCards.isNotEmpty()) {
                builder.appendLine("  <selectedCards>")
                selectedCards.forEach { index ->
                    builder.appendLine("    <index>$index</index>")
                }
                builder.appendLine("  </selectedCards>")
            }
            builder.appendLine()

            if (movementHistory.isNotEmpty()) {
                builder.appendLine("  <movementHistory>")
                movementHistory.forEach { movement ->
                    builder.appendLine("    <movement>")
                    builder.appendLine("      <moveNumber>${movement.moveNumber}</moveNumber>")
                    builder.appendLine("      <player><![CDATA[${movement.player}]]></player>")
                    builder.appendLine("      <card1Index>${movement.card1Index}</card1Index>")
                    builder.appendLine("      <card2Index>${movement.card2Index}</card2Index>")
                    builder.appendLine("      <wasMatch>${movement.wasMatch}</wasMatch>")
                    builder.appendLine("    </movement>")
                }
                builder.appendLine("  </movementHistory>")
            }

            builder.appendLine("</SavedGame>")

            builder.toString()
        } catch (e: Exception) {
            android.util.Log.e("SavedGame", "Error en toXmlFormat", e)
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><error>${e.message}</error>"
        }
    }

    /**
     * Convierte el objeto a formato JSON
     */
    fun toJsonFormat(): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val json = JSONObject()

            json.put("id", id)
            json.put("gameName", gameName)
            json.put("gameMode", gameMode)
            json.put("timestamp", dateFormat.format(Date(timestamp)))
            json.put("themeColor", themeColor)

            val players = JSONObject()
            val player1 = JSONObject()
            player1.put("name", player1Name)
            player1.put("score", player1Score)
            players.put("player1", player1)

            if (player2Name != null && player2Score != null) {
                val player2 = JSONObject()
                player2.put("name", player2Name)
                player2.put("score", player2Score)
                players.put("player2", player2)
            }

            players.put("currentPlayer", currentPlayer)
            json.put("players", players)

            val gameState = JSONObject()
            gameState.put("movesCount", movesCount)
            gameState.put("pairsFound", pairsFound)
            gameState.put("totalPairs", totalPairs)
            if (lives != null) {
                gameState.put("lives", lives)
            }
            gameState.put("timeElapsed", timeElapsed)
            gameState.put("isCheckingMatch", isCheckingMatch)
            json.put("gameState", gameState)

            val cardsArray = JSONArray()
            cards.forEach { card ->
                val cardObj = JSONObject()
                cardObj.put("id", card.id)
                cardObj.put("pairId", card.pairId)
                cardObj.put("colorHex", card.colorHex)
                cardObj.put("isFlipped", card.isFlipped)
                cardObj.put("isMatched", card.isMatched)
                cardsArray.put(cardObj)
            }
            json.put("cards", cardsArray)

            if (selectedCards.isNotEmpty()) {
                val selectedArray = JSONArray()
                selectedCards.forEach { selectedArray.put(it) }
                json.put("selectedCards", selectedArray)
            } else {
                json.put("selectedCards", JSONArray())
            }

            if (movementHistory.isNotEmpty()) {
                val historyArray = JSONArray()
                movementHistory.forEach { movement ->
                    val moveObj = JSONObject()
                    moveObj.put("moveNumber", movement.moveNumber)
                    moveObj.put("player", movement.player)
                    moveObj.put("card1Index", movement.card1Index)
                    moveObj.put("card2Index", movement.card2Index)
                    moveObj.put("wasMatch", movement.wasMatch)
                    historyArray.put(moveObj)
                }
                json.put("movementHistory", historyArray)
            } else {
                json.put("movementHistory", JSONArray())
            }

            json.toString(2)
        } catch (e: Exception) {
            android.util.Log.e("SavedGame", "Error en toJsonFormat", e)
            "{\"error\": \"${e.message}\"}"
        }
    }

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

    companion object {
        /**
         * Parsea un archivo TXT y devuelve un SavedGame
         */
        fun fromTxtFormat(content: String): SavedGame? {
            return try {
                val lines = content.lines()
                val data = mutableMapOf<String, String>()

                lines.forEach { line ->
                    if (line.contains(":") && !line.startsWith("===")) {
                        val parts = line.split(":", limit = 2)
                        if (parts.size == 2) {
                            data[parts[0].trim()] = parts[1].trim()
                        }
                    }
                }

                // Parsear cartas
                val cards = mutableListOf<SavedCard>()
                lines.filter { it.startsWith("Carta ") }.forEach { line ->
                    val pairId = line.substringAfter("pairId=").substringBefore(",").toInt()
                    val colorHex = line.substringAfter("color=").substringBefore(",")
                    val isFlipped = line.substringAfter("volteada=").substringBefore(",").toBoolean()
                    val isMatched = line.substringAfter("emparejada=").toBoolean()

                    cards.add(SavedCard(cards.size, pairId, colorHex, isFlipped, isMatched))
                }

                SavedGame(
                    id = data["ID"] ?: UUID.randomUUID().toString(),
                    gameName = data["Nombre"] ?: "Partida",
                    gameMode = data["Modo de juego"] ?: "LOCAL_2PLAYERS",
                    player1Name = data["Jugador 1"]?.substringBefore(" -") ?: "Jugador 1",
                    player2Name = data["Jugador 2"]?.substringBefore(" -"),
                    player1Score = data["Jugador 1"]?.substringAfter("Puntuación: ")?.toIntOrNull() ?: 0,
                    player2Score = data["Jugador 2"]?.substringAfter("Puntuación: ")?.toIntOrNull(),
                    currentPlayer = data["Turno actual"] ?: "PLAYER1",
                    movesCount = data["Movimientos realizados"]?.toIntOrNull() ?: 0,
                    pairsFound = data["Parejas encontradas"]?.substringBefore(" de")?.toIntOrNull() ?: 0,
                    totalPairs = data["Parejas encontradas"]?.substringAfter(" de ")?.toIntOrNull() ?: 8,
                    lives = data["Vidas restantes"]?.toIntOrNull(),
                    timeElapsed = parseTime(data["Tiempo transcurrido"] ?: "00:00"),
                    cards = cards,
                    selectedCards = emptyList(),
                    isCheckingMatch = data["Verificando coincidencia"]?.contains("Sí") == true,
                    themeColor = data["Tema"] ?: "AZUL"
                )
            } catch (e: Exception) {
                android.util.Log.e("SavedGame", "Error parsing TXT: ${e.message}")
                null
            }
        }

        private fun parseTime(timeStr: String): Long {
            val parts = timeStr.split(":")
            return when (parts.size) {
                2 -> {
                    val minutes = parts[0].toLongOrNull() ?: 0
                    val seconds = parts[1].toLongOrNull() ?: 0
                    (minutes * 60 + seconds) * 1000
                }
                3 -> {
                    val hours = parts[0].toLongOrNull() ?: 0
                    val minutes = parts[1].toLongOrNull() ?: 0
                    val seconds = parts[2].toLongOrNull() ?: 0
                    (hours * 3600 + minutes * 60 + seconds) * 1000
                }
                else -> 0
            }
        }
    }
}

/**
 * Data class que representa una carta guardada
 */

data class SavedCard(
    val id: Int,
    val pairId: Int,
    val colorHex: String,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)

/**
 * Data class que representa un movimiento en el historial
 */

data class Movement(
    val moveNumber: Int,
    val player: String,
    val card1Index: Int,
    val card2Index: Int,
    val wasMatch: Boolean
)

/**
 * Metadatos de una partida guardada
 */
data class SavedGameMetadata(
    val id: String,
    val gameName: String,
    val gameMode: String,
    val timestamp: Long,
    val player1Name: String,
    val player2Name: String?,
    val format: SaveFormat,
    val fileName: String,
    val tags: List<String> = emptyList()
)