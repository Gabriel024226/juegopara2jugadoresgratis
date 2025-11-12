package com.example.paintbrawl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paintbrawl.model.SaveFormat
import com.example.paintbrawl.model.ThemeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    currentTheme: ThemeColor,
    onThemeChange: (ThemeColor) -> Unit,
    currentSaveFormat: SaveFormat,
    onSaveFormatChange: (SaveFormat) -> Unit
) {
    val gradient = when (currentTheme) {
        ThemeColor.GUINDA -> listOf(Color(0xFF6C1D45), Color(0xFF9B2D5E))
        ThemeColor.AZUL -> listOf(Color(0xFF1A237E), Color(0xFF311B92))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradient))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Configuración", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sección de tema
                SettingsSection(title = "Apariencia") {
                    SettingsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Tema de Color",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Selecciona el tema visual de la aplicación",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ThemeOption(
                                    theme = ThemeColor.GUINDA,
                                    selected = currentTheme == ThemeColor.GUINDA,
                                    onClick = { onThemeChange(ThemeColor.GUINDA) },
                                    modifier = Modifier.weight(1f)
                                )

                                ThemeOption(
                                    theme = ThemeColor.AZUL,
                                    selected = currentTheme == ThemeColor.AZUL,
                                    onClick = { onThemeChange(ThemeColor.AZUL) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Sección de guardado
                SettingsSection(title = "Guardado de Partidas") {
                    SettingsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Formato de Archivo Preferido",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Las partidas se guardarán en este formato por defecto",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            SaveFormat.values().forEach { format ->
                                SaveFormatOption(
                                    format = format,
                                    selected = currentSaveFormat == format,
                                    onClick = { onSaveFormatChange(format) }
                                )
                                if (format != SaveFormat.values().last()) {
                                    Divider(
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Información de la app
                SettingsSection(title = "Información") {
                    SettingsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow(
                                label = "Versión",
                                value = "1.0.0"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(
                                label = "Desarrollado por",
                                value = "ESCOM-IPN"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        content()
    }
}

@Composable
fun ThemeOption(
    theme: ThemeColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                Color.White.copy(alpha = 0.3f)
            else
                Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (selected)
            androidx.compose.foundation.BorderStroke(2.dp, Color.White)
        else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(theme.primaryColor),
                                    Color(theme.secondaryColor)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (theme) {
                        ThemeColor.GUINDA -> "Guinda"
                        ThemeColor.AZUL -> "Azul"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SaveFormatOption(
    format: SaveFormat,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = Color.White.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = format.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = when (format) {
                    SaveFormat.TXT -> "Texto plano, fácil de leer y editar"
                    SaveFormat.XML -> "Formato estructurado, compatible universalmente"
                    SaveFormat.JSON -> "Formato compacto, ideal para intercambio de datos"
                },
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Icon(
            imageVector = when (format) {
                SaveFormat.TXT -> Icons.Default.Description
                SaveFormat.XML -> Icons.Default.Code
                SaveFormat.JSON -> Icons.Default.DataObject
            },
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}