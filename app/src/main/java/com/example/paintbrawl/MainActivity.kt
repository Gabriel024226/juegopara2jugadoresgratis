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
import com.example.paintbrawl.ui.screens.GameScreen
import com.example.paintbrawl.ui.screens.MenuScreen
import com.example.paintbrawl.ui.theme.PaintbrawlTheme
import com.example.paintbrawl.viewmodel.GameViewModel
import com.example.paintbrawl.viewmodel.GameViewModelFactory
import kotlinx.coroutines.delay

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
            PaintbrawlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: GameViewModel = viewModel(
                        factory = GameViewModelFactory(applicationContext)
                    )

                    var currentScreen by remember { mutableStateOf("menu") }
                    val connectionState by viewModel.connectionState.collectAsState()
                    val gameState by viewModel.gameState.collectAsState()

                    // Observar el estado de conexión Bluetooth
                    LaunchedEffect(connectionState) {
                        android.util.Log.d("MainActivity", "Estado conexión cambió a: $connectionState")

                        if (connectionState == BluetoothGameManager.ConnectionState.CONNECTED) {
                            // Esperar un momento para asegurar sincronización
                            delay(500)
                            if (currentScreen == "menu") {
                                android.util.Log.d("MainActivity", "Cambiando a pantalla de juego")
                                currentScreen = "game"
                            }
                        }

                        // Si se desconecta durante el juego, volver al menú
                        if (connectionState == BluetoothGameManager.ConnectionState.DISCONNECTED
                            && currentScreen == "game"
                            && gameState.gameMode == GameMode.BLUETOOTH
                            && !gameState.isGameOver) {
                            android.util.Log.d("MainActivity", "Conexión perdida, volviendo al menú")
                            delay(1000)
                            currentScreen = "menu"
                        }
                    }

                    when (currentScreen) {
                        "menu" -> MenuScreen(
                            onStartGame = { gameMode, isHost ->
                                android.util.Log.d("MainActivity", "Iniciando juego - Modo: $gameMode, Host: $isHost")

                                if (gameMode == GameMode.LOCAL) {
                                    viewModel.startGame(gameMode, 8, isHost)
                                    currentScreen = "game"
                                } else {
                                    // En modo Bluetooth, iniciar el ViewModel
                                    // La transición a "game" se hará cuando se conecte
                                    viewModel.startGame(gameMode, 8, isHost)
                                }
                            },
                            onShowStats = {
                                currentScreen = "stats"
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
                            }
                        )

                        "game" -> {
                            // Mostrar pantalla de espera si está en modo Bluetooth y no está conectado
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
                                        android.util.Log.d("MainActivity", "Volviendo al menú, desconectando Bluetooth")

                                        // Desconectar Bluetooth antes de volver al menú
                                        if (gameState.gameMode == GameMode.BLUETOOTH) {
                                            viewModel.disconnectBluetooth()
                                        }

                                        currentScreen = "menu"
                                    }
                                )
                            }
                        }

                        "stats" -> com.example.paintbrawl.ui.screens.StatsScreen(
                            onBack = {
                                currentScreen = "menu"
                            }
                        )
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