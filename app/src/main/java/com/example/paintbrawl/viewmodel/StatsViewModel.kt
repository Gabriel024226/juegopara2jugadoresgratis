package com.example.paintbrawl.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paintbrawl.data.GameHistory
import com.example.paintbrawl.data.GameRepository
import com.example.paintbrawl.data.PlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel para manejar las estadísticas y el historial
 */
class StatsViewModel(private val repository: GameRepository) : ViewModel() {

    val playerStats: Flow<List<PlayerStats>> = repository.allStats
    val gameHistory: Flow<List<GameHistory>> = repository.recentGames

    /**
     * Borra todos los datos de estadísticas
     */
    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }
}

/**
 * Factory para crear el StatsViewModel
 */
class StatsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(GameRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}