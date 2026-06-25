package com.example.mislibros.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.viewmodel.AuthViewModel

@Composable
fun InfoScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onProfileClick: () -> Unit
) {
    val role = authViewModel.currentUserProfile?.role
    val isAdmin = role == "ADMIN"

    LibraryScaffold(
        authViewModel = authViewModel,
        title = "Información de la App",
        isAdmin = isAdmin,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = onBackClick
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Header / Title inside Card
                        Text(
                            text = "MisLibros",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = YaleBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "Tu biblioteca digital al alcance de tu mano",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 24.dp)
                        )

                        // ¿Quiénes Somos?
                        InfoSection(
                            title = "¿Quiénes Somos?",
                            content = "Somos una plataforma digital innovadora dedicada a conectar a lectores con el maravilloso mundo de los libros. Facilitamos la gestión y consulta de catálogos y préstamos de forma rápida y sencilla.",
                            icon = Icons.Default.Groups,
                            iconColor = YaleBlue
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Misión
                        InfoSection(
                            title = "Misión",
                            content = "Fomentar el hábito de la lectura y el libre acceso a la información a través de una aplicación intuitiva y eficiente, simplificando la administración de bibliotecas y mejorando la experiencia de los usuarios.",
                            icon = Icons.Default.Info,
                            iconColor = AirForceBlue
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Visión
                        InfoSection(
                            title = "Visión",
                            content = "Ser el software de gestión bibliotecaria de referencia en el ámbito escolar y comunitario, promoviendo el conocimiento y el desarrollo cultural mediante la digitalización de recursos.",
                            icon = Icons.Default.Visibility,
                            iconColor = AmberOrange
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Spacer(modifier = Modifier.height(20.dp))

                        // Contacto Header
                        Text(
                            text = "Contacto y Soporte",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = YaleBlue,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Teléfono row
                        ContactRow(
                            icon = Icons.Default.Phone,
                            label = "Número de Contacto",
                            value = "+52 55 1234 5678",
                            iconColor = YaleBlue
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Correo row
                        ContactRow(
                            icon = Icons.Default.Email,
                            label = "Correo Electrónico",
                            value = "contacto@mislibros.com",
                            iconColor = YaleBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: String,
    icon: ImageVector,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Justify
            )
        }
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
