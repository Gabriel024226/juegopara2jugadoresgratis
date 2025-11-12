package com.example.paintbrawl.data

import android.content.Context
import com.example.paintbrawl.model.SaveFormat
import com.example.paintbrawl.model.SavedGame
import com.example.paintbrawl.model.SavedGameMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import java.io.StringReader

/**
 * Manager para gestionar el guardado y carga de partidas
 */
class SaveGameManager(private val context: Context) {

    private val savesDirectory: File
        get() = File(context.filesDir, "saves").apply {
            if (!exists()) mkdirs()
        }

    /**
     * Guarda una partida en el formato especificado
     */
    suspend fun saveGame(
        savedGame: SavedGame,
        format: SaveFormat,
        tags: List<String> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val extension = when (format) {
                SaveFormat.TXT -> "txt"
                SaveFormat.XML -> "xml"
                SaveFormat.JSON -> "json"
            }

            val fileName = "${savedGame.gameName.replace(" ", "_")}_${System.currentTimeMillis()}.$extension"
            val file = File(savesDirectory, fileName)

            val content = when (format) {
                SaveFormat.TXT -> savedGame.toTxtFormat()
                SaveFormat.XML -> savedGame.toXmlFormat()
                SaveFormat.JSON -> savedGame.toJsonFormat()
            }

            FileOutputStream(file).use { output ->
                output.write(content.toByteArray())
            }

            // Guardar metadatos en un archivo separado
            saveMetadata(savedGame, fileName, format, tags)

            Result.success(fileName)
        } catch (e: Exception) {
            android.util.Log.e("SaveGameManager", "Error saving game: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Carga una partida desde un archivo
     */
    suspend fun loadGame(fileName: String): Result<SavedGame> = withContext(Dispatchers.IO) {
        try {
            val file = File(savesDirectory, fileName)

            if (!file.exists()) {
                return@withContext Result.failure(Exception("El archivo no existe"))
            }

            val content = FileInputStream(file).use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }

            val extension = file.extension
            val savedGame = when (extension) {
                "txt" -> SavedGame.fromTxtFormat(content)
                "xml" -> parseXmlSavedGame(content)
                "json" -> parseJsonSavedGame(content)
                else -> null
            }

            savedGame?.let {
                Result.success(it)
            } ?: Result.failure(Exception("No se pudo parsear el archivo"))

        } catch (e: Exception) {
            android.util.Log.e("SaveGameManager", "Error loading game: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene todas las partidas guardadas
     */
    suspend fun getAllSavedGames(): List<SavedGameMetadata> = withContext(Dispatchers.IO) {
        val metadataFile = File(savesDirectory, "metadata.json")

        if (!metadataFile.exists()) {
            return@withContext emptyList()
        }

        try {
            val content = FileInputStream(metadataFile).use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }

            val jsonArray = org.json.JSONArray(content)
            val metadataList = mutableListOf<SavedGameMetadata>()

            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)

                // Verificar que el archivo aún existe
                val fileName = jsonObj.getString("fileName")
                if (File(savesDirectory, fileName).exists()) {
                    metadataList.add(
                        SavedGameMetadata(
                            id = jsonObj.getString("id"),
                            gameName = jsonObj.getString("gameName"),
                            gameMode = jsonObj.getString("gameMode"),
                            timestamp = jsonObj.getLong("timestamp"),
                            player1Name = jsonObj.getString("player1Name"),
                            player2Name = jsonObj.optString("player2Name").takeIf { it.isNotEmpty() },
                            format = SaveFormat.valueOf(jsonObj.getString("format")),
                            fileName = fileName,
                            tags = jsonObj.optJSONArray("tags")?.let { tagsArray ->
                                List(tagsArray.length()) { tagsArray.getString(it) }
                            } ?: emptyList()
                        )
                    )
                }
            }

            metadataList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            android.util.Log.e("SaveGameManager", "Error loading metadata: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Elimina una partida guardada
     */
    suspend fun deleteSavedGame(fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(savesDirectory, fileName)
            if (file.exists()) {
                file.delete()
            }

            // Actualizar metadatos
            val allMetadata = getAllSavedGames()
            val updatedMetadata = allMetadata.filter { it.fileName != fileName }
            saveAllMetadata(updatedMetadata)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exporta una partida al almacenamiento externo compartido
     */
    suspend fun exportGame(fileName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(savesDirectory, fileName)
            val exportsDir = File(context.getExternalFilesDir(null), "MemoramaExports").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(exportsDir, fileName)

            sourceFile.copyTo(destFile, overwrite = true)

            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lee el contenido de un archivo guardado como texto
     */
    suspend fun readFileContent(fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(savesDirectory, fileName)
            val content = FileInputStream(file).use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveMetadata(
        savedGame: SavedGame,
        fileName: String,
        format: SaveFormat,
        tags: List<String>
    ) {
        val metadataFile = File(savesDirectory, "metadata.json")

        val existingMetadata = if (metadataFile.exists()) {
            FileInputStream(metadataFile).use { input ->
                org.json.JSONArray(input.readBytes().toString(Charsets.UTF_8))
            }
        } else {
            org.json.JSONArray()
        }

        val newMetadata = JSONObject()
        newMetadata.put("id", savedGame.id)
        newMetadata.put("gameName", savedGame.gameName)
        newMetadata.put("gameMode", savedGame.gameMode)
        newMetadata.put("timestamp", savedGame.timestamp)
        newMetadata.put("player1Name", savedGame.player1Name)
        savedGame.player2Name?.let { newMetadata.put("player2Name", it) }
        newMetadata.put("format", format.name)
        newMetadata.put("fileName", fileName)

        val tagsArray = org.json.JSONArray()
        tags.forEach { tagsArray.put(it) }
        newMetadata.put("tags", tagsArray)

        existingMetadata.put(newMetadata)

        FileOutputStream(metadataFile).use { output ->
            output.write(existingMetadata.toString(2).toByteArray())
        }
    }

    private fun saveAllMetadata(metadataList: List<SavedGameMetadata>) {
        val metadataFile = File(savesDirectory, "metadata.json")
        val jsonArray = org.json.JSONArray()

        metadataList.forEach { metadata ->
            val jsonObj = JSONObject()
            jsonObj.put("id", metadata.id)
            jsonObj.put("gameName", metadata.gameName)
            jsonObj.put("gameMode", metadata.gameMode)
            jsonObj.put("timestamp", metadata.timestamp)
            jsonObj.put("player1Name", metadata.player1Name)
            metadata.player2Name?.let { jsonObj.put("player2Name", it) }
            jsonObj.put("format", metadata.format.name)
            jsonObj.put("fileName", metadata.fileName)

            val tagsArray = org.json.JSONArray()
            metadata.tags.forEach { tagsArray.put(it) }
            jsonObj.put("tags", tagsArray)

            jsonArray.put(jsonObj)
        }

        FileOutputStream(metadataFile).use { output ->
            output.write(jsonArray.toString(2).toByteArray())
        }
    }

    private fun parseXmlSavedGame(content: String): SavedGame? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc: Document = builder.parse(InputSource(StringReader(content)))
            doc.documentElement.normalize()

            val root = doc.documentElement

            fun getElementText(tagName: String, parent: Element = root): String? {
                return parent.getElementsByTagName(tagName).item(0)?.textContent
            }

            val playersElement = root.getElementsByTagName("players").item(0) as Element
            val gameStateElement = root.getElementsByTagName("gameState").item(0) as Element

            // Parsear cartas
            val cardsElement = root.getElementsByTagName("cards").item(0) as Element
            val cardNodes = cardsElement.getElementsByTagName("card")
            val cards = mutableListOf<com.example.paintbrawl.model.SavedCard>()

            for (i in 0 until cardNodes.length) {
                val cardElement = cardNodes.item(i) as Element
                cards.add(
                    com.example.paintbrawl.model.SavedCard(
                        id = getElementText("id", cardElement)?.toInt() ?: i,
                        pairId = getElementText("pairId", cardElement)?.toInt() ?: 0,
                        colorHex = getElementText("colorHex", cardElement) ?: "#000000",
                        isFlipped = getElementText("isFlipped", cardElement)?.toBoolean() ?: false,
                        isMatched = getElementText("isMatched", cardElement)?.toBoolean() ?: false
                    )
                )
            }

            SavedGame(
                id = getElementText("id") ?: "",
                gameName = getElementText("gameName") ?: "",
                gameMode = getElementText("gameMode") ?: "LOCAL_2PLAYERS",
                player1Name = getElementText("n", playersElement.getElementsByTagName("player1").item(0) as Element) ?: "Jugador 1",
                player2Name = playersElement.getElementsByTagName("player2").item(0)?.let {
                    getElementText("n", it as Element)
                },
                player1Score = getElementText("score", playersElement.getElementsByTagName("player1").item(0) as Element)?.toInt() ?: 0,
                player2Score = playersElement.getElementsByTagName("player2").item(0)?.let {
                    getElementText("score", it as Element)?.toInt()
                },
                currentPlayer = getElementText("currentPlayer", playersElement) ?: "PLAYER1",
                movesCount = getElementText("movesCount", gameStateElement)?.toInt() ?: 0,
                pairsFound = getElementText("pairsFound", gameStateElement)?.toInt() ?: 0,
                totalPairs = getElementText("totalPairs", gameStateElement)?.toInt() ?: 8,
                lives = getElementText("lives", gameStateElement)?.toIntOrNull(),
                timeElapsed = getElementText("timeElapsed", gameStateElement)?.toLong() ?: 0,
                cards = cards,
                selectedCards = emptyList(),
                isCheckingMatch = getElementText("isCheckingMatch", gameStateElement)?.toBoolean() ?: false,
                themeColor = getElementText("themeColor") ?: "AZUL"
            )
        } catch (e: Exception) {
            android.util.Log.e("SaveGameManager", "Error parsing XML: ${e.message}", e)
            null
        }
    }

    private fun parseJsonSavedGame(content: String): SavedGame? {
        return try {
            val json = JSONObject(content)

            val players = json.getJSONObject("players")
            val player1 = players.getJSONObject("player1")
            val player2 = players.optJSONObject("player2")

            val gameState = json.getJSONObject("gameState")

            val cardsArray = json.getJSONArray("cards")
            val cards = mutableListOf<com.example.paintbrawl.model.SavedCard>()

            for (i in 0 until cardsArray.length()) {
                val cardObj = cardsArray.getJSONObject(i)
                cards.add(
                    com.example.paintbrawl.model.SavedCard(
                        id = cardObj.getInt("id"),
                        pairId = cardObj.getInt("pairId"),
                        colorHex = cardObj.getString("colorHex"),
                        isFlipped = cardObj.getBoolean("isFlipped"),
                        isMatched = cardObj.getBoolean("isMatched")
                    )
                )
            }

            SavedGame(
                id = json.getString("id"),
                gameName = json.getString("gameName"),
                gameMode = json.getString("gameMode"),
                player1Name = player1.getString("name"),
                player2Name = player2?.getString("name"),
                player1Score = player1.getInt("score"),
                player2Score = player2?.getInt("score"),
                currentPlayer = players.getString("currentPlayer"),
                movesCount = gameState.getInt("movesCount"),
                pairsFound = gameState.getInt("pairsFound"),
                totalPairs = gameState.getInt("totalPairs"),
                lives = gameState.optInt("lives").takeIf { gameState.has("lives") },
                timeElapsed = gameState.getLong("timeElapsed"),
                cards = cards,
                selectedCards = emptyList(),
                isCheckingMatch = gameState.getBoolean("isCheckingMatch"),
                themeColor = json.getString("themeColor")
            )
        } catch (e: Exception) {
            android.util.Log.e("SaveGameManager", "Error parsing JSON: ${e.message}", e)
            null
        }
    }
}