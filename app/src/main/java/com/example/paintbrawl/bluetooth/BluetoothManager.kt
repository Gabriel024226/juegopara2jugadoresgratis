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
            return
        }

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
            return
        }

        connectThread?.cancel()
        connectThread = ConnectThread(device)
        connectThread?.start()
        _connectionState.value = ConnectionState.CONNECTING
    }

    /**
     * Envía un mensaje a través de Bluetooth
     */
    fun sendMessage(message: String) {
        connectedThread?.write(message.toByteArray())
    }

    /**
     * Obtiene la lista de dispositivos emparejados
     */
    fun getPairedDevices(): Set<BluetoothDevice> {
        if (!hasBluetoothPermission()) {
            return emptySet()
        }
        return bluetoothAdapter?.bondedDevices ?: emptySet()
    }

    /**
     * Desconecta y limpia recursos
     */
    fun disconnect() {
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
                    bluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
                } else null
            } catch (e: IOException) {
                null
            }
        }

        override fun run() {
            var socket: BluetoothSocket? = null
            while (_connectionState.value != ConnectionState.CONNECTED) {
                try {
                    socket = serverSocket?.accept()
                } catch (e: IOException) {
                    break
                }

                socket?.let {
                    manageConnectedSocket(it)
                    serverSocket?.close()
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {
                // Error al cerrar el socket
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
                    device.createRfcommSocketToServiceRecord(MY_UUID)
                } else null
            } catch (e: IOException) {
                null
            }
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()

            try {
                socket?.connect()
                socket?.let { manageConnectedSocket(it) }
            } catch (e: IOException) {
                try {
                    socket?.close()
                } catch (closeException: IOException) {
                    // Error al cerrar el socket
                }
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                // Error al cerrar el socket
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
            _connectionState.value = ConnectionState.CONNECTED
            var numBytes: Int

            while (true) {
                try {
                    numBytes = inputStream?.read(buffer) ?: break
                    val message = String(buffer, 0, numBytes)
                    _receivedMessage.value = message
                } catch (e: IOException) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream?.write(bytes)
            } catch (e: IOException) {
                // Error al enviar datos
            }
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                // Error al cerrar el socket
            }
        }
    }

    /**
     * Maneja un socket conectado
     */
    private fun manageConnectedSocket(socket: BluetoothSocket) {
        connectedThread?.cancel()
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()
    }
}