package com.example.paintbrawl.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Entidad que representa las estadísticas de un jugador
 */
@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerName: String,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val totalPairsFound: Int = 0,
    val totalMoves: Int = 0,
    val bestScore: Int = 0,
    val lastPlayed: Long = System.currentTimeMillis()
)

/**
 * Entidad que representa el historial de partidas
 */
@Entity(tableName = "game_history")
data class GameHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val player1Name: String,
    val player2Name: String,
    val player1Score: Int,
    val player2Score: Int,
    val winnerName: String?,
    val totalMoves: Int,
    val gameMode: String,
    val date: Long = System.currentTimeMillis()
)

/**
 * DAO para acceder a las estadísticas de los jugadores
 */
@Dao
interface PlayerStatsDao {
    @Query("SELECT * FROM player_stats ORDER BY gamesWon DESC")
    fun getAllStats(): Flow<List<PlayerStats>>

    @Query("SELECT * FROM player_stats WHERE playerName = :name LIMIT 1")
    suspend fun getStatsByName(name: String): PlayerStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: PlayerStats): Long

    @Update
    suspend fun updateStats(stats: PlayerStats)

    @Query("DELETE FROM player_stats")
    suspend fun deleteAllStats()

    @Query("SELECT * FROM player_stats WHERE id = :id")
    suspend fun getStatsById(id: Long): PlayerStats?
}

/**
 * DAO para acceder al historial de partidas
 */
@Dao
interface GameHistoryDao {
    @Query("SELECT * FROM game_history ORDER BY date DESC LIMIT 50")
    fun getRecentGames(): Flow<List<GameHistory>>

    @Query("SELECT * FROM game_history ORDER BY date DESC")
    fun getAllGames(): Flow<List<GameHistory>>

    @Insert
    suspend fun insertGame(game: GameHistory): Long

    @Query("DELETE FROM game_history")
    suspend fun deleteAllHistory()

    @Query("SELECT COUNT(*) FROM game_history WHERE winnerName = :playerName")
    suspend fun getWinsForPlayer(playerName: String): Int
}

/**
 * Base de datos Room para el juego
 */
@Database(
    entities = [PlayerStats::class, GameHistory::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GameDatabase : RoomDatabase() {
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun gameHistoryDao(): GameHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "memorama_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Converters para tipos personalizados
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

/**
 * Repositorio para manejar las operaciones de base de datos
 */
class GameRepository(context: Context) {
    private val database = GameDatabase.getDatabase(context)
    private val playerStatsDao = database.playerStatsDao()
    private val gameHistoryDao = database.gameHistoryDao()

    val allStats: Flow<List<PlayerStats>> = playerStatsDao.getAllStats()
    val recentGames: Flow<List<GameHistory>> = gameHistoryDao.getRecentGames()

    suspend fun updatePlayerStats(
        playerName: String,
        won: Boolean,
        pairsFound: Int,
        moves: Int,
        score: Int
    ) {
        val existingStats = playerStatsDao.getStatsByName(playerName)

        if (existingStats != null) {
            val updatedStats = existingStats.copy(
                gamesPlayed = existingStats.gamesPlayed + 1,
                gamesWon = existingStats.gamesWon + if (won) 1 else 0,
                totalPairsFound = existingStats.totalPairsFound + pairsFound,
                totalMoves = existingStats.totalMoves + moves,
                bestScore = maxOf(existingStats.bestScore, score),
                lastPlayed = System.currentTimeMillis()
            )
            playerStatsDao.updateStats(updatedStats)
        } else {
            val newStats = PlayerStats(
                playerName = playerName,
                gamesPlayed = 1,
                gamesWon = if (won) 1 else 0,
                totalPairsFound = pairsFound,
                totalMoves = moves,
                bestScore = score,
                lastPlayed = System.currentTimeMillis()
            )
            playerStatsDao.insertStats(newStats)
        }
    }

    suspend fun saveGameHistory(
        player1Name: String,
        player2Name: String,
        player1Score: Int,
        player2Score: Int,
        winner: String?,
        totalMoves: Int,
        gameMode: String
    ) {
        val gameHistory = GameHistory(
            player1Name = player1Name,
            player2Name = player2Name,
            player1Score = player1Score,
            player2Score = player2Score,
            winnerName = winner,
            totalMoves = totalMoves,
            gameMode = gameMode
        )
        gameHistoryDao.insertGame(gameHistory)
    }

    suspend fun getPlayerStats(playerName: String): PlayerStats? {
        return playerStatsDao.getStatsByName(playerName)
    }

    suspend fun clearAllData() {
        playerStatsDao.deleteAllStats()
        gameHistoryDao.deleteAllHistory()
    }
}