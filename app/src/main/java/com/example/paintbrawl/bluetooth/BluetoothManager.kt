package com.example.paintbrawl.bluetooth

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Manager para manejar la comunicación Bluetooth entre dos dispositivos
 */
class BluetoothGameManager(private val context: Context) {

    companion object {
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val NAME = "MemoramaGame"
        private const val TAG = "BluetoothGameManager"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var acceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _receivedMessage = MutableStateFlow<String?>(null)
    val receivedMessage: StateFlow<String?> = _receivedMessage.asStateFlow()

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    /**
     * Inicia el servidor Bluetooth para aceptar conexiones
     */
    fun startServer() {
        if (!hasBluetoothPermission()) {
            android.util.Log.e(TAG, "No hay permisos de Bluetooth")
            return
        }

        android.util.Log.d(TAG, "Iniciando servidor Bluetooth")
        acceptThread?.cancel()
        acceptThread = AcceptThread()
        acceptThread?.start()
        _connectionState.value = ConnectionState.CONNECTING
    }

    /**
     * Conecta a un dispositivo Bluetooth como cliente
     */
    fun connectToDevice(device: BluetoothDevice) {
        if (!hasBluetoothPermission()) {
            android.util.Log.e(TAG, "No hay permisos de Bluetooth")
            return
        }

        android.util.Log.d(TAG, "Conectando a dispositivo: ${device.name}")
        connectThread?.cancel()
        connectThread = ConnectThread(device)
        connectThread?.start()
        _connectionState.value = ConnectionState.CONNECTING
    }

    /**
     * Envía un mensaje a través de Bluetooth
     */
    fun sendMessage(message: String) {
        android.util.Log.d(TAG, "Intentando enviar mensaje: $message")
        if (connectedThread == null) {
            android.util.Log.e(TAG, "No hay conexión establecida (connectedThread es null)")
        } else {
            connectedThread?.write(message.toByteArray())
        }
    }

    /**
     * Obtiene la lista de dispositivos emparejados
     */
    fun getPairedDevices(): Set<BluetoothDevice> {
        if (!hasBluetoothPermission()) {
            android.util.Log.e(TAG, "No hay permisos de Bluetooth para obtener dispositivos")
            return emptySet()
        }
        val devices = bluetoothAdapter?.bondedDevices ?: emptySet()
        android.util.Log.d(TAG, "Dispositivos emparejados: ${devices.size}")
        return devices
    }

    /**
     * Desconecta y limpia recursos
     */
    fun disconnect() {
        android.util.Log.d(TAG, "Desconectando...")
        acceptThread?.cancel()
        connectThread?.cancel()
        connectedThread?.cancel()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Verifica si tiene los permisos necesarios de Bluetooth
     */
    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Thread para aceptar conexiones entrantes (Servidor)
     */
    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket? by lazy {
            try {
                if (hasBluetoothPermission()) {
                    android.util.Log.d(TAG, "Creando servidor socket")
                    bluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
                } else {
                    android.util.Log.e(TAG, "Sin permisos para crear servidor")
                    null
                }
            } catch (e: IOException) {
                android.util.Log.e(TAG, "Error al crear servidor socket", e)
                null
            }
        }

        override fun run() {
            android.util.Log.d(TAG, "AcceptThread iniciado, esperando conexión...")
            var socket: BluetoothSocket? = null

            // Solo intentar aceptar una conexión
            try {
                socket = serverSocket?.accept()
                android.util.Log.d(TAG, "Conexión aceptada!")
            } catch (e: IOException) {
                android.util.Log.e(TAG, "Error al aceptar conexión", e)
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            socket?.let {
                android.util.Log.d(TAG, "Socket conectado, iniciando ConnectedThread")
                manageConnectedSocket(it)
            }

            // Cerrar el servidor socket después de aceptar la conexión
            try {
                serverSocket?.close()
                android.util.Log.d(TAG, "Servidor socket cerrado")
            } catch (e: IOException) {
                android.util.Log.e(TAG, "Error al cerrar servidor socket", e)
            }

            android.util.Log.d(TAG, "AcceptThread finalizado")
        }

        fun cancel() {
            try {
                serverSocket?.close()
                android.util.Log.d(TAG, "Servidor socket cerrado (cancel)")
            } catch (e: IOException) {
                android.util.Log.e(TAG, "Error al cerrar servidor socket (cancel)", e)
            }
        }
    }

    /**
     * Thread para iniciar conexión a un dispositivo (Cliente)
     */
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket? by lazy {
            try {
                if (hasBluetoothPermission()) {
                    android.util.Log.d(TAG, "Creando socket cliente")
                    device.createRfcommSocketToServiceRecord(MY_UUID)
                } else {
                    android.util.Log.e(TAG, "Sin permisos para crear socket")
                    null
                }
            } catch (e: IOException) {
                android.util.Log.e(TAG, "Error al crear socket cliente", e)
                null
            }
        }

        override fun run() {
            android.util.Log.d(TAG, "ConnectThread iniciado")
            bluetoothAdapter?.cancelDiscovery()

            try {
                android.util.Log.d(TAG, "Intentando conectar...")
                socket?.connect()
                android.util.Log.d(TAG, "Conectado! Iniciando ConnectedThread")
                socket?.let { manageConnectedSocket(it) }
            } catch (e: IOException) {
                android.util.Log.e(TAG, "Error al conectar", e)
                try {
                    socket?.close()
                } catch (closeException: IOException) {
                    android.util.Log.e(TAG, "Error al cerrar socket", closeException)
                }
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        fun cancel() {
            try {
                socket?.close()
                android.util.Log.d(TAG, "Socket cliente cerrado")
            } catch (e: IOException) {
                android.util.Log.e(TAG, "Error al cerrar socket cliente", e)
            }
        }
    }

    /**
     * Thread para manejar la conexión establecida
     */
    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream? = socket.inputStream
        private val outputStream: OutputStream? = socket.outputStream
        private val buffer = ByteArray(1024)

        override fun run() {
            android.util.Log.d(TAG, "ConnectedThread iniciado")
            _connectionState.value = ConnectionState.CONNECTED
            var numBytes: Int

            while (true) {
                try {
                    numBytes = inputStream?.read(buffer) ?: break
                    val message = String(buffer, 0, numBytes)
                    android.util.Log.d(TAG, "Mensaje recibido en ConnectedThread: $message")
                    _receivedMessage.value = message
                } catch (e: IOException) {
                    android.util.Log.e(TAG, "Error al leer mensaje", e)
                    _connectionState.value = ConnectionState.DISCONNECTED
                    break
                }
            }
            android.util.Log.d(TAG, "ConnectedThread finalizado")
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream?.write(bytes)
                outputStream?.flush()
                android.util.Log.d(TAG, "Mensaje enviado: ${String(bytes)}")
            } catch (e: IOException) {
                android.util.Log.e(TAG, "Error al enviar mensaje", e)
            }
        }

        fun cancel() {
            try {
                socket.close()
                android.util.Log.d(TAG, "Socket conectado cerrado")
            } catch (e: IOException) {
                android.util.Log.e(TAG, "Error al cerrar socket", e)
            }
        }
    }

    /**
     * Maneja un socket conectado
     */
    private fun manageConnectedSocket(socket: BluetoothSocket) {
        android.util.Log.d(TAG, "Gestionando socket conectado")
        connectedThread?.cancel()
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()
    }
}