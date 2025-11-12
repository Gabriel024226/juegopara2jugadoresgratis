package com.example.paintbrawl

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paintbrawl.bluetooth.BluetoothGameManager
import com.example.paintbrawl.model.GameMode
import com.example.paintbrawl.ui.components.SaveErrorDialog
import com.example.paintbrawl.ui.components.SaveGameDialog
import com.example.paintbrawl.ui.components.SaveSuccessDialog
import com.example.paintbrawl.ui.screens.GameScreen
import com.example.paintbrawl.ui.screens.MenuScreen
import com.example.paintbrawl.ui.screens.SavedGamesScreen
import com.example.paintbrawl.ui.screens.SettingsScreen
import com.example.paintbrawl.ui.screens.StatsScreen
import com.example.paintbrawl.ui.theme.PaintbrawlTheme
import com.example.paintbrawl.viewmodel.GameViewModel
import com.example.paintbrawl.viewmodel.GameViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothManager?.adapter
    }

    private val requestBluetoothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            android.util.Log.d("MainActivity", "Permisos Bluetooth concedidos")
        } else {
            android.util.Log.w("MainActivity", "Permisos Bluetooth denegados")
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("MainActivity", "Resultado activación Bluetooth: ${result.resultCode}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBluetoothPermissions()

        setContent {
            val viewModel: GameViewModel = viewModel(
                factory = GameViewModelFactory(applicationContext)
            )

            val currentTheme by viewModel.currentTheme.collectAsState()
            val preferredSaveFormat by viewModel.preferredSaveFormat.collectAsState()

            PaintbrawlTheme(currentThemeColor = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("menu") }
                    var showSaveDialog by remember { mutableStateOf(false) }
                    var saveResult by remember { mutableStateOf<Result<String>?>(null) }
                    val coroutineScope = rememberCoroutineScope()

                    val connectionState by viewModel.connectionState.collectAsState()
                    val gameState by viewModel.gameState.collectAsState()

                    // Observar el estado de conexión Bluetooth
                    LaunchedEffect(connectionState) {
                        android.util.Log.d("MainActivity", "Estado conexión cambió a: $connectionState")

                        if (connectionState == BluetoothGameManager.ConnectionState.CONNECTED) {
                            delay(500)
                            if (currentScreen == "menu") {
                                android.util.Log.d("MainActivity", "Cambiando a pantalla de juego")
                                currentScreen = "game"
                            }
                        }

                        if (connectionState == BluetoothGameManager.ConnectionState.DISCONNECTED
                            && currentScreen == "game"
                            && gameState.gameMode == GameMode.BLUETOOTH
                            && !gameState.isGameOver) {
                            android.util.Log.d("MainActivity", "Conexión perdida, volviendo al menú")
                            delay(1000)
                            currentScreen = "menu"
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            "menu" -> MenuScreen(
                                onStartGame = { gameMode, isHost, theme ->
                                    android.util.Log.d("MainActivity", "Iniciando juego - Modo: $gameMode, Host: $isHost")

                                    viewModel.setTheme(theme)

                                    if (gameMode == GameMode.LOCAL || gameMode == GameMode.CLARIVIDENTE) {
                                        viewModel.startGame(gameMode, 8, isHost, theme)
                                        currentScreen = "game"
                                    } else if (gameMode == GameMode.BLUETOOTH) {
                                        viewModel.startGame(gameMode, 8, isHost, theme)
                                    }
                                },
                                onShowStats = {
                                    currentScreen = "stats"
                                },
                                onShowSavedGames = {
                                    currentScreen = "saved_games"
                                },
                                onShowSettings = {
                                    currentScreen = "settings"
                                },
                                bluetoothAdapter = bluetoothAdapter,
                                onStartBluetoothServer = {
                                    android.util.Log.d("MainActivity", "Iniciando servidor Bluetooth")
                                    viewModel.startBluetoothServer()
                                },
                                onConnectToDevice = { device ->
                                    android.util.Log.d("MainActivity", "Conectando a dispositivo: ${device.name}")
                                    viewModel.connectToDevice(device)
                                },
                                getPairedDevices = {
                                    viewModel.getPairedDevices()
                                },
                                currentTheme = currentTheme
                            )

                            "game" -> {
                                if (gameState.gameMode == GameMode.BLUETOOTH &&
                                    connectionState != BluetoothGameManager.ConnectionState.CONNECTED) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                        Text(
                                            text = if (gameState.isBluetoothHost)
                                                "Esperando jugador 2..."
                                            else
                                                "Conectando...",
                                            modifier = Modifier.align(Alignment.BottomCenter)
                                        )
                                    }
                                } else {
                                    GameScreen(
                                        viewModel = viewModel,
                                        onBackToMenu = {
                                            android.util.Log.d("MainActivity", "Volviendo al menú")

                                            if (gameState.gameMode == GameMode.BLUETOOTH) {
                                                viewModel.disconnectBluetooth()
                                            }

                                            currentScreen = "menu"
                                        },
                                        onSaveGame = {
                                            if (gameState.gameMode != GameMode.BLUETOOTH) {
                                                android.util.Log.d("MainActivity", "Mostrando diálogo de guardado")
                                                showSaveDialog = true
                                            } else {
                                                android.util.Log.w("MainActivity", "No se puede guardar en modo Bluetooth")
                                            }
                                        }
                                    )
                                }

                                // Diálogo de guardado
                                if (showSaveDialog) {
                                    SaveGameDialog(
                                        onDismiss = {
                                            android.util.Log.d("MainActivity", "Diálogo de guardado cancelado")
                                            showSaveDialog = false
                                        },
                                        onSave = { name, format, tags ->
                                            android.util.Log.d("MainActivity", "Guardando: name=$name, format=$format, tags=$tags")
                                            viewModel.setSaveFormat(format)
                                            coroutineScope.launch {
                                                try {
                                                    android.util.Log.d("MainActivity", "Iniciando guardado...")
                                                    val result = viewModel.saveCurrentGame(name, tags)
                                                    android.util.Log.d("MainActivity", "Resultado: ${if (result.isSuccess) "Éxito" else "Error"}")
                                                    saveResult = result
                                                    showSaveDialog = false
                                                } catch (e: Exception) {
                                                    android.util.Log.e("MainActivity", "Excepción al guardar", e)
                                                    saveResult = Result.failure(e)
                                                    showSaveDialog = false
                                                }
                                            }
                                        },
                                        currentFormat = preferredSaveFormat
                                    )
                                }

                                // Diálogo de resultado de guardado
                                saveResult?.let { result ->
                                    result.fold(
                                        onSuccess = { fileName ->
                                            android.util.Log.d("MainActivity", "Mostrando diálogo de éxito: $fileName")
                                            SaveSuccessDialog(
                                                fileName = fileName,
                                                onDismiss = {
                                                    android.util.Log.d("MainActivity", "Cerrando diálogo de éxito")
                                                    saveResult = null
                                                }
                                            )
                                        },
                                        onFailure = { error ->
                                            android.util.Log.e("MainActivity", "Mostrando diálogo de error: ${error.message}")
                                            SaveErrorDialog(
                                                error = error.message ?: "Error desconocido",
                                                onDismiss = {
                                                    android.util.Log.d("MainActivity", "Cerrando diálogo de error")
                                                    saveResult = null
                                                }
                                            )
                                        }
                                    )
                                }
                            }

                            "saved_games" -> SavedGamesScreen(
                                onBack = {
                                    currentScreen = "menu"
                                },
                                onLoadGame = { fileName ->
                                    coroutineScope.launch {
                                        val result = viewModel.loadSavedGame(fileName)
                                        result.fold(
                                            onSuccess = {
                                                currentScreen = "game"
                                            },
                                            onFailure = { error ->
                                                android.util.Log.e("MainActivity", "Error cargando: ${error.message}")
                                            }
                                        )
                                    }
                                }
                            )

                            "settings" -> SettingsScreen(
                                onBack = {
                                    currentScreen = "menu"
                                },
                                currentTheme = currentTheme,
                                onThemeChange = { theme ->
                                    viewModel.setTheme(theme)
                                },
                                currentSaveFormat = preferredSaveFormat,
                                onSaveFormatChange = { format ->
                                    viewModel.setSaveFormat(format)
                                }
                            )

                            "stats" -> StatsScreen(
                                onBack = {
                                    currentScreen = "menu"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        if (!hasBluetoothPermissions(permissions)) {
            requestBluetoothPermissions.launch(permissions)
        }
    }

    private fun hasBluetoothPermissions(permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun enableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("MainActivity", "onDestroy - limpiando recursos")
    }
}