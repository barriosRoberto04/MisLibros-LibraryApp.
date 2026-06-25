package com.example.mislibros.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.model.UserModel
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.SunnyYellow
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.viewmodel.AuthViewModel
import com.example.mislibros.ui.components.LibraryScaffold
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.example.mislibros.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

@Composable
fun AdminQueryUsersScreen(
    authViewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit,
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    onEditUserClick: (UserModel) -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val logoutSuccessText = stringResource(id = R.string.logout_success)
    val userProfile = authViewModel.currentUserProfile

    var userList by remember { mutableStateOf<List<UserModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estado para confirmación de eliminación
    var userToDelete by remember { mutableStateOf<UserModel?>(null) }

    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }

    // Cargar usuarios en tiempo real
    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance().getReference("usuarios")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserModel>()
                for (child in snapshot.children) {
                    val user = child.getValue(UserModel::class.java)
                    if (user != null) {
                        list.add(user)
                    }
                }
                userList = list
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                Toast.makeText(context, "Error al cargar usuarios: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        database.addValueEventListener(listener)
    }

    // Diálogo de confirmación de eliminación
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Text(
                    text = "¿Esta seguro de eliminar a este usuario?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Esta acción lo borrara permanentemente de la base de datos",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val user = userToDelete!!
                        userToDelete = null
                        
                        // 1. Eliminar de Realtime Database
                        val dbRef = FirebaseDatabase.getInstance().getReference("usuarios").child(user.userId)
                        dbRef.removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Usuario eliminado de la base de datos", Toast.LENGTH_SHORT).show()
                                
                                // 2. Intentar borrar foto de Storage
                                val storageRef = FirebaseStorage.getInstance().reference.child("profile_photos/${user.userId}.jpg")
                                storageRef.delete()
                                    .addOnSuccessListener {
                                        // Borrado de storage exitoso
                                    }
                                    .addOnFailureListener {
                                        // Ignorar si no tenía foto o falló
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error al eliminar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    }
                ) {
                    Text(text = "ELIMINAR", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(text = "CANCELAR", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    LibraryScaffold(
        authViewModel = authViewModel,
        title = "Consultar Usuarios",
        isAdmin = true,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = onBackClick
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Buscador por Nombre y botón con icono de buscar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar usuario por nombre...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YaleBlue,
                            unfocusedBorderColor = YaleBlue.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    IconButton(
                        onClick = { focusManager.clearFocus() },
                        modifier = Modifier
                            .size(54.dp)
                            .background(YaleBlue, shape = RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color.White
                        )
                    }
                }

                // Filtrar por nombre y ordenar por orden alfabético
                val filteredAndSortedList = remember(userList, searchQuery) {
                    userList
                        .filter { user ->
                            val fullName = "${user.nombre} ${user.apellidos}".trim()
                            fullName.contains(searchQuery, ignoreCase = true)
                        }
                        .sortedBy { "${it.nombre} ${it.apellidos}".trim().uppercase() }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                    }
                } else if (filteredAndSortedList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) "No hay usuarios registrados" else "No se encontraron usuarios",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredAndSortedList) { user ->
                            UserListItem(
                                user = user,
                                onEdit = { onEditUserClick(user) },
                                onDelete = { userToDelete = user }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(
    user: UserModel,
    onEdit: () -> Unit,
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
            // Sección izquierda: Foto y Nombre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Foto de perfil
                if (user.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.imageUrl,
                        contentDescription = "Foto de ${user.nombre}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                } else {
                    // Si el usuario no tiene foto, extrae la primera letra de su nombre (ej: "Roberto" -> "R")
                    val initial = user.nombre.firstOrNull()?.toString() ?: "U"
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(AirForceBlue, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial.uppercase(), // Pinta la inicial en mayusculas al centro del circulo
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${user.nombre} ${user.apellidos}".trim(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val roleLabel = if (user.role.equals("ADMIN", ignoreCase = true)) "Administrador" else "Normal"
                    val roleColor = if (user.role.equals("ADMIN", ignoreCase = true)) AmberOrange else SunnyYellow
                    Text(
                        text = roleLabel,
                        fontSize = 12.sp,
                        color = roleColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Sección derecha: Iconos editar/eliminar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = YaleBlue
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}
