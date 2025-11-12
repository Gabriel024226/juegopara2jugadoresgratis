package com.example.paintbrawl.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paintbrawl.data.SaveGameManager
import com.example.paintbrawl.model.SaveFormat
import com.example.paintbrawl.model.SavedGameMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedGamesViewModel(private val context: Context) : ViewModel() {

    private val saveGameManager = SaveGameManager(context)

    private val _savedGames = MutableStateFlow<List<SavedGameMetadata>>(emptyList())
    val savedGames: StateFlow<List<SavedGameMetadata>> = _savedGames.asStateFlow()

    private val _selectedFormat = MutableStateFlow<SaveFormat?>(null)
    val selectedFormat: StateFlow<SaveFormat?> = _selectedFormat.asStateFlow()

    val preferredFormat = MutableStateFlow(SaveFormat.JSON)

    private var allSavedGames: List<SavedGameMetadata> = emptyList()

    fun loadSavedGames() {
        viewModelScope.launch {
            allSavedGames = saveGameManager.getAllSavedGames()
            filterByFormat(_selectedFormat.value)
        }
    }

    fun filterByFormat(format: SaveFormat?) {
        _selectedFormat.value = format
        _savedGames.value = if (format == null) {
            allSavedGames
        } else {
            allSavedGames.filter { it.format == format }
        }
    }

    fun deleteGame(fileName: String) {
        viewModelScope.launch {
            saveGameManager.deleteSavedGame(fileName)
            loadSavedGames()
        }
    }

    fun exportGame(fileName: String) {
        viewModelScope.launch {
            val result = saveGameManager.exportGame(fileName)
            result.fold(
                onSuccess = {
                    android.util.Log.d("SavedGamesViewModel", "Partida exportada: ${it.absolutePath}")
                },
                onFailure = {
                    android.util.Log.e("SavedGamesViewModel", "Error exportando: ${it.message}")
                }
            )
        }
    }

    fun getFileContent(fileName: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = saveGameManager.readFileContent(fileName)
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { onResult("Error al leer el archivo: ${it.message}") }
            )
        }
    }

    fun setPreferredFormat(format: SaveFormat) {
        preferredFormat.value = format
    }
}

class SavedGamesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedGamesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavedGamesViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}