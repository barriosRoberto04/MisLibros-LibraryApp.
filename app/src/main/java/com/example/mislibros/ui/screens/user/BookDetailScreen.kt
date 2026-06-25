package com.example.mislibros.ui.screens.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.model.BookLoanModel
import com.example.mislibros.model.BookModel
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.ui.theme.YaleBlue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

@Composable
fun BookDetailScreen(
    authViewModel: com.example.mislibros.viewmodel.AuthViewModel,
    book: BookModel?,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onProfileClick: () -> Unit,
    onRequestLoanClick: () -> Unit,
    onEditBookClick: ((BookModel) -> Unit)? = null,
    onDeleteBookSuccess: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val userProfile = authViewModel.currentUserProfile
    val isAdmin = userProfile?.role == "ADMIN"

    // Estados para diálogos de eliminación
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isCheckingLoans by remember { mutableStateOf(false) }
    var showActiveLoanError by remember { mutableStateOf(false) }
    var activeLoanCount by remember { mutableStateOf(0) }

    // Función para verificar préstamos y eliminar
    fun checkLoansAndDelete() {
        if (book == null) return
        isCheckingLoans = true
        val loansRef = FirebaseDatabase.getInstance().getReference("prestamos")
        loansRef.orderByChild("bookId").equalTo(book.bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isCheckingLoans = false
                    val activeStatuses = listOf("PENDIENTE", "ACTIVO", "VENCIDO")
                    val activeCount = snapshot.children.count { child ->
                        val loan = child.getValue(BookLoanModel::class.java)
                        loan != null && loan.status in activeStatuses
                    }
                    if (activeCount > 0) {
                        activeLoanCount = activeCount
                        showActiveLoanError = true
                    } else {
                        // Sin préstamos activos eliminar libro
                        val dbRef = FirebaseDatabase.getInstance()
                            .getReference("libros").child(book.bookId)
                        dbRef.removeValue().addOnSuccessListener {
                            Toast.makeText(context, "Libro eliminado del catálogo", Toast.LENGTH_SHORT).show()
                            if (book.imageUrl.isNotEmpty()) {
                                FirebaseStorage.getInstance().reference
                                    .child("book_covers/${book.bookId}.jpg")
                                    .delete().addOnSuccessListener {}.addOnFailureListener {}
                            }
                            if (book.authorImageUrl.isNotEmpty()) {
                                FirebaseStorage.getInstance().reference
                                    .child("author_photos/${book.bookId}.jpg")
                                    .delete().addOnSuccessListener {}.addOnFailureListener {}
                            }
                            onDeleteBookSuccess?.invoke()
                        }.addOnFailureListener { e ->
                            Toast.makeText(context, "Error al eliminar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    isCheckingLoans = false
                    Toast.makeText(context, "Error al verificar préstamos: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // DIÁLOGO CONFIRMACIÓN DE BORRADO
    if (showDeleteConfirm && book != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(text = "¿Eliminar este libro?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Esta acción borrará permanentemente el libro y su portada del catálogo.",
                        fontSize = 14.sp
                    )
                    Text(
                        text = " ${book.title}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = YaleBlue
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    checkLoansAndDelete()
                }) {
                    Text(text = "ELIMINAR", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = "CANCELAR", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // DIÁLOGO DE ERROR PRÉSTAMOS ACTIVOS
    if (showActiveLoanError) {
        AlertDialog(
            onDismissRequest = { showActiveLoanError = false },
            title = {
                Text(
                    text = "No se puede eliminar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFE53935)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(32.dp).align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Este libro tiene $activeLoanCount préstamo(s) activo(s), pendiente(s) o vencido(s).",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Solo se puede eliminar cuando todos sus préstamos hayan sido devueltos.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showActiveLoanError = false }) {
                    Text(text = "ENTENDIDO", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // VERIFICANDO PRÉSTAMOS
    if (isCheckingLoans) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Verificando préstamos...", fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = YaleBlue)
                }
            },
            confirmButton = {},
            shape = RoundedCornerShape(24.dp)
        )
    }

    LibraryScaffold(
        authViewModel = authViewModel,
        title = "Detalle del Libro",
        isAdmin = isAdmin,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = onBackClick
    ) { innerPadding ->
        if (book == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se pudo cargar la información del libro",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // PORTADA GRANDE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        YaleBlue.copy(alpha = 0.05f),
                                        YaleBlue.copy(alpha = 0.15f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (book.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = book.imageUrl,
                                contentDescription = "Portada de ${book.title}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, YaleBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = YaleBlue,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // TÍTULO Y AUTOR + BOTONES ADMIN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = YaleBlue,
                                lineHeight = 30.sp
                            )
                            Text(
                                text = "por ${book.author}",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // BOTONES ADMIN (editar / eliminar)
                        if (isAdmin) {
                            Row {
                                IconButton(onClick = { onEditBookClick?.invoke(book) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar libro",
                                        tint = YaleBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar libro",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // FICHA TÉCNICA
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Ficha Técnica",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = YaleBlue,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            DetailRow(label = "Categoría", value = book.category, icon = Icons.Default.Category)
                            DetailRow(label = "Editorial", value = book.publisher, icon = Icons.Default.Business)
                            DetailRow(label = "Año", value = book.year, icon = Icons.Default.CalendarToday)
                            DetailRow(label = "Edición", value = book.edition, icon = Icons.Default.Layers)
                            DetailRow(label = "Idioma", value = book.language, icon = Icons.Default.Language)
                            DetailRow(label = "Páginas", value = book.pages, icon = Icons.Default.AutoStories)
                            DetailRow(label = "Ejemplares", value = book.stock, icon = Icons.Default.Inventory)
                            DetailRow(
                                label = "Estatus",
                                value = if (book.status == "AVAILABLE") "Disponible" else book.status,
                                icon = Icons.Default.Info
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // RESEÑA
                    SectionHeader(text = "Reseña del Libro")
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    ) {
                        Text(
                            text = book.review.ifBlank { "No hay reseña disponible para este libro." },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // ACERCA DEL AUTOR
                    SectionHeader(text = "Acerca del Autor")
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(YaleBlue.copy(alpha = 0.1f), shape = CircleShape)
                                        .border(1.5.dp, YaleBlue, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (book.authorImageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = book.authorImageUrl,
                                            contentDescription = "Foto de ${book.author}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = YaleBlue,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = book.author,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = YaleBlue
                                    )
                                    Text(
                                        text = "Autor principal",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = book.authorBio.ifBlank { "No hay bibliografía disponible para este autor." },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    // BOTÓN PEDIR PRESTADO (solo usuarios normales)
                    if (!isAdmin) {
                        Button(
                            onClick = onRequestLoanClick,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberOrange,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LibraryAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SOLICITAR PRÉSTAMO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        // Botones admin en la parte inferior también
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onEditBookClick?.invoke(book) },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, YaleBlue),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = YaleBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EDITAR",
                                    fontWeight = FontWeight.Bold,
                                    color = YaleBlue
                                )
                            }
                            Button(
                                onClick = { showDeleteConfirm = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ELIMINAR",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AirForceBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$label:",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(85.dp)
        )
        Text(
            text = value.ifBlank { "No especificado" },
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = YaleBlue,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
