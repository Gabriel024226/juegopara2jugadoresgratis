package com.example.paintbrawl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paintbrawl.model.SaveFormat

/**
 * Diálogo principal para guardar una partida
 */
@Composable
fun SaveGameDialog(
    onDismiss: () -> Unit,
    onSave: (String, SaveFormat, List<String>) -> Unit,
    currentFormat: SaveFormat
) {
    var gameName by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(currentFormat) }
    var tagsText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Guardar Partida",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nombre de la partida
                OutlinedTextField(
                    value = gameName,
                    onValueChange = {
                        gameName = it
                        showError = false
                    },
                    label = { Text("Nombre de la partida") },
                    placeholder = { Text("Ej: Mi mejor partida") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("El nombre no puede estar vacío", color = Color.Red) }
                    } else null
                )

                // Selector de formato
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Formato de archivo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SaveFormat.values().forEach { format ->
                            FilterChip(
                                selected = selectedFormat == format,
                                onClick = { selectedFormat = format },
                                label = { Text(format.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = when (selectedFormat) {
                            SaveFormat.TXT -> "• Texto plano, fácil de leer\n• Compatible con cualquier editor"
                            SaveFormat.XML -> "• Formato estructurado\n• Ampliamente compatible"
                            SaveFormat.JSON -> "• Formato compacto\n• Ideal para intercambio de datos"
                        },
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Etiquetas
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Etiquetas (opcional)") },
                    placeholder = { Text("Ej: difícil, récord, casual") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Separa las etiquetas con comas", fontSize = 12.sp)
                    },
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (gameName.isBlank()) {
                        showError = true
                    } else {
                        val tags = tagsText.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        onSave(gameName, selectedFormat, tags)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Diálogo que muestra éxito al guardar una partida
 */
@Composable
fun SaveSuccessDialog(
    fileName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = "✓",
                fontSize = 48.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
        },
        title = {
            Text(
                text = "¡Partida Guardada!",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "La partida se ha guardado correctamente",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Gray.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = fileName,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Puedes continuar jugando o cargar esta partida más tarde desde el menú",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Continuar jugando")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Diálogo que muestra error al guardar una partida
 */
@Composable
fun SaveErrorDialog(
    error: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = "⚠",
                fontSize = 48.sp,
                color = Color(0xFFFF5722)
            )
        },
        title = {
            Text(
                text = "Error al Guardar",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color(0xFFFF5722)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No se pudo guardar la partida:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF5722).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = Color(0xFFFF5722),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Por favor, inténtalo de nuevo o verifica el almacenamiento disponible.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5722)
                )
            ) {
                Text("Entendido")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}