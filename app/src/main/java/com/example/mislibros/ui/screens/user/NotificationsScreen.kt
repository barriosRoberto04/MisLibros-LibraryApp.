package com.example.mislibros.ui.screens.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.model.NotificationModel
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.viewmodel.AuthViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun NotificationsScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val userProfile = authViewModel.currentUserProfile
    val isAdmin = userProfile?.role == "ADMIN"
    val currentUserId = userProfile?.userId ?: ""

    var notifications by remember { mutableStateOf<List<NotificationModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Escuchar notificaciones en tiempo real, filtrando por rol
    LaunchedEffect(userProfile) {
        if (userProfile == null) {
            isLoading = false
            return@LaunchedEffect
        }
        FirebaseDatabase.getInstance().getReference("notificaciones")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<NotificationModel>()
                    for (child in snapshot.children) {
                        val notif = child.getValue(NotificationModel::class.java)
                        if (notif != null) {
                            // Filtrar según rol
                            val pertenece = if (isAdmin) {
                                notif.targetRole == "ADMIN"
                            } else {
                                notif.targetRole == "USER" && notif.targetUserId == currentUserId
                            }
                            if (pertenece) list.add(notif)
                        }
                    }
                    notifications = list.sortedByDescending { it.timestamp }
                    isLoading = false
                }
                override fun onCancelled(error: DatabaseError) {
                    isLoading = false
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    LibraryScaffold(
        authViewModel = authViewModel,
        title = "Notificaciones",
        isAdmin = isAdmin,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = onBackClick
    ) { innerPadding ->
        Column(
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Botón "Limpiar todo" solo si hay notificaciones
            if (notifications.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        // Eliminar solo las notificaciones que pertenecen al usuario actual
                        val db = FirebaseDatabase.getInstance().getReference("notificaciones")
                        notifications.forEach { notif ->
                            db.child(notif.notificationId).removeValue()
                        }
                        Toast.makeText(context, "Notificaciones eliminadas", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Limpiar todo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Lista de notificaciones
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                }
            } else if (notifications.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No hay notificaciones",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (isAdmin)
                                "Aquí aparecerán las solicitudes de préstamo\nde los usuarios."
                            else
                                "Aquí aparecerán las actualizaciones\nde tus préstamos.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(notifications, key = { it.notificationId }) { notif ->
                        NotificationCard(
                            notif = notif,
                            isAdmin = isAdmin,
                            onDelete = {
                                FirebaseDatabase.getInstance().getReference("notificaciones")
                                    .child(notif.notificationId).removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Notificación eliminada", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notif: NotificationModel,
    isAdmin: Boolean,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sección izquierda: Imagen + Detalles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Si es Administrador, dibuja la foto de perfil circular del usuario que solicita
                if (isAdmin) {
                    if (notif.userImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = notif.userImageUrl, // <-- Invoca la foto del usuario usando su URL de Firebase Storage
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        if (notif.title.contains("Reporte", ignoreCase = true)) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(AirForceBlue.copy(alpha = 0.15f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Feedback,
                                    contentDescription = null,
                                    tint = AirForceBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            // Limpia los prefijos de prestamo del titulo para obtener el nombre del solicitante
                            val namePart = notif.title
                                .removePrefix("Préstamo:")
                                .removePrefix("Prestamo:")
                                .trim()
                            // Toma la primera letra del nombre limpio del usuario
                            val initial = namePart.firstOrNull()?.toString() ?: "P"
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(AirForceBlue, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial.uppercase(), // Renderiza la inicial al centro del circulo de color
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                } else {
                    // Usuario normal: portada del libro (rectangular) o reporte (Feedback)
                    if (notif.bookImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = notif.bookImageUrl,
                            contentDescription = "Portada del libro",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 46.dp, height = 60.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        val isReport = notif.title.contains("Reporte", ignoreCase = true) ||
                            notif.title.contains("Estatus", ignoreCase = true) ||
                            notif.message.contains("reporte", ignoreCase = true)
                        // Notificaciones de estatus de reporte: icono amarillo
                        val isStatusUpdate = notif.title.contains("Estatus", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 60.dp)
                                .background(
                                    if (isStatusUpdate) AmberOrange.copy(alpha = 0.15f)
                                    else AirForceBlue.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isReport) Icons.Default.Feedback else Icons.Default.Book,
                                contentDescription = null,
                                tint = if (isStatusUpdate) AmberOrange else AirForceBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    // Invoca el titulo de la notificacion (ej: "Prestamo Solicitado")
                    Text(
                        text = notif.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Invoca el mensaje descriptivo (ej: "Solicita el libro")

                    Text(
                        text = notif.message,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    // Solo mostrar fecha/hora en notificaciones de préstamos, no de reportes
                    val isReportNotif = notif.title.contains("Reporte", ignoreCase = true) ||
                        notif.title.startsWith("Estatus:", ignoreCase = true) ||
                        notif.message.contains("reporte", ignoreCase = true)
                    if (!isReportNotif) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notif.timestamp,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Botón de eliminar individual
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar notificación",
                    tint = Color(0xFFE53935)
                )
            }
        }
    }
}
