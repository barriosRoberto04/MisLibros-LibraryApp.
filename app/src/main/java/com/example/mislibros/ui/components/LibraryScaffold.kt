package com.example.mislibros.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.R
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.ui.theme.SunnyYellow
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.viewmodel.AuthViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.example.mislibros.model.NotificationModel
import com.example.mislibros.ui.navigation.LocalNavController
import com.example.mislibros.ui.navigation.AppScreen
import androidx.compose.material.icons.automirrored.filled.MenuBook

@Composable
fun LibraryScaffold(
    authViewModel: AuthViewModel,
    title: String? = null,
    isAdmin: Boolean = false,  // Si isMandatory es true, se deshabilita la opción de ir al Home y el botón Regresar
    onHomeClick: (() -> Unit)? = null,
    onProfileClick: (/* ya estamos en perfil */) -> Unit,
    onLogoutSuccess: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    // Se oculta la barra de navegación inferior
    onNotificationsClick: (() -> Unit)? = null,
    showBottomBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val logoutSuccessText = stringResource(id = R.string.logout_success)
    val userProfile = authViewModel.currentUserProfile
    val comingSoonText = stringResource(id = R.string.coming_soon)

    var isHelpDialogOpen by remember { mutableStateOf(false) }
    var isLogoutDialogOpen by remember { mutableStateOf(false) }

    // El contador de notificaciones no leidas empieza en 0 por defecto
    var unreadCount by remember { mutableStateOf(0) }

    // Escucha en tiempo real de Firebase para actualizar el contador de notificaciones de forma automatica
    LaunchedEffect(userProfile) {
        if (userProfile == null) return@LaunchedEffect
        val databaseRef = FirebaseDatabase.getInstance().getReference("notificaciones")
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                for (child in snapshot.children) {
                    val notif = child.getValue(NotificationModel::class.java)
                    if (notif != null) {
                        // Si es administrador, suma las notificaciones dirigidas al rol ADMIN
                        if (isAdmin && notif.targetRole == "ADMIN") {
                            count++
                        // Si es usuario normal, suma solo las dirigidas al rol USER y a su userId especifico
                        } else if (!isAdmin && notif.targetRole == "USER" && notif.targetUserId == userProfile.userId) {
                            count++
                        }
                    }
                }
                unreadCount = count // Actualiza el estado reactivo que dibuja el circulo rojo
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Diálogo de Información según el rol
    if (isHelpDialogOpen) {
        AlertDialog(
            onDismissRequest = { isHelpDialogOpen = false },
            title = {
                Text(
                    text = stringResource(id = R.string.info_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                if (isAdmin) {
                    Text(
                        text = stringResource(id = R.string.info_dialog_body),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Bienvenido a la biblioteca digital. Aquí puedes realizar tus consultas y préstamos.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Preguntas Frecuentes:\n" +
                                    "1. ¿Cómo buscar libros?\nUtiliza el botón \"Buscar libros\" en tu panel principal.\n\n" +
                                    "2. ¿Cómo solicitar un libro prestado?\nIngresa en \"Solicitar libros\", busca tu título y confirma la solicitud.\n\n" +
                                    "3. ¿Dónde veo mis préstamos vigentes?\nPulsa en \"Consulta de préstamos\" para ver el estado de tus libros e historial.\n\n" +
                                    "Soporte: biblioteca@mislibros.com",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isHelpDialogOpen = false }) {
                    Text(text = stringResource(id = R.string.close_button), fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }



    // Diálogo de confirmación de Cerrar Sesión
    if (isLogoutDialogOpen) {
        AlertDialog(
            onDismissRequest = { isLogoutDialogOpen = false },
            title = {
                Text(
                    text = "Cerrar Sesión",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro que deseas cerrar sesión?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isLogoutDialogOpen = false
                        authViewModel.signOut()
                        Toast.makeText(context, logoutSuccessText, Toast.LENGTH_SHORT).show()
                        onLogoutSuccess()
                    }
                ) {
                    Text(
                        text = "Cerrar Sesión",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { isLogoutDialogOpen = false }) {
                    Text(text = "Cancelar", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YaleBlue)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp, bottom = 8.dp, start = if (title != null) 8.dp else 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (title != null) Arrangement.Start else Arrangement.SpaceBetween
                ) {
                    if (title != null) {
                        // Modo con Botón Regresar y Título
                        if (onBackClick != null) {
                            IconButton(onClick = { onBackClick.invoke() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Regresar",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        // Modo Inicio (Foto y nombre a la izquierda, Notificaciones a la derecha)
                        val paternalLastName = userProfile?.apellidos?.trim()?.split(Regex("\\s+"))?.firstOrNull() ?: ""
                        val nameAndPaternal = if (paternalLastName.isNotEmpty()) {
                            "${userProfile?.nombre ?: ""} $paternalLastName"
                        } else {
                            userProfile?.nombre ?: ""
                        }
                        val displayName = if (isAdmin) "Administrador: $nameAndPaternal" else nameAndPaternal

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Profile picture and name (Left side)
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Se carga la foto del perfil desde la URL publica de Storage, si no existe se muestra un icono por defecto
                                if (!userProfile?.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = userProfile?.imageUrl,
                                        contentDescription = "Perfil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    // Icono generico si no hay foto de perfil cargada en la cuenta
                                    Icon(
                                        imageVector = Icons.Filled.AccountCircle,
                                        contentDescription = "Perfil",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Notifications icon and Clear All button (Right side)
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Notifications icon with badge
                                IconButton(onClick = { onNotificationsClick?.invoke() }) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadCount > 0) {
                                                Badge(
                                                    containerColor = Color.Red,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(unreadCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = "Notificaciones",
                                            tint = SunnyYellow
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // Separador
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )
            }
        },
        bottomBar = {
            if (showBottomBar && !isKeyboardOpen) {
                NavigationBar(
                    containerColor = YaleBlue,
                    tonalElevation = 8.dp
                ) {
                    val navBarColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmberOrange,
                        selectedTextColor = AmberOrange,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = AirForceBlue.copy(alpha = 0.3f)
                    )

                    // 1. Botón Perfil
                    NavigationBarItem(
                        selected = false,
                        onClick = { onProfileClick() },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.bottom_profile), fontSize = 10.sp) },
                        colors = navBarColors
                    )

                    // 2. Botón Inicio
                    NavigationBarItem(
                        selected = false,
                        onClick = { onHomeClick?.invoke() },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.bottom_home), fontSize = 10.sp) },
                        colors = navBarColors
                    )

                    // 3. Botón Ayuda (Información)
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            if (navController != null) {
                                navController.navigate(AppScreen.Info.route)
                            } else {
                                isHelpDialogOpen = true
                            }
                        },
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.bottom_info), fontSize = 10.sp) },
                        colors = navBarColors
                    )

                    // 4. Botón Cerrar Sesión
                    NavigationBarItem(
                        selected = false,
                        onClick = { isLogoutDialogOpen = true },
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.bottom_logout), fontSize = 10.sp) },
                        colors = navBarColors
                    )
                }
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}
