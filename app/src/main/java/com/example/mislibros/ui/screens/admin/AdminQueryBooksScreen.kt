package com.example.mislibros.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.R
import com.example.mislibros.model.BookLoanModel
import com.example.mislibros.model.BookModel
import com.example.mislibros.ui.components.PremiumTextField
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.ui.theme.SunnyYellow
import com.example.mislibros.viewmodel.AuthViewModel
import com.example.mislibros.ui.components.LibraryScaffold
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

@Composable
fun AdminQueryBooksScreen(
    authViewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit,
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    onEditBookClick: (BookModel) -> Unit,
    onBookClick: (BookModel) -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val bookDeleteSuccess = stringResource(id = R.string.book_delete_success)
    val userProfile = authViewModel.currentUserProfile

    var bookList by remember { mutableStateOf<List<BookModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados para diálogos
    var bookToDelete by remember { mutableStateOf<BookModel?>(null) }
    var isCheckingLoans by remember { mutableStateOf(false) }
    var showActiveLoanError by remember { mutableStateOf(false) }
    var activeLoanCount by remember { mutableStateOf(0) }

    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }

    // CARGAR LIBROS EN TIEMPO REAL
    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance().getReference("libros")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BookModel>()
                for (child in snapshot.children) {
                    val book = child.getValue(BookModel::class.java)
                    if (book != null) list.add(book)
                }
                bookList = list
                isLoading = false
            }
            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        database.addValueEventListener(listener)
    }

    // Función reutilizable: verifica préstamos activos y procede a borrar o muestra error
    fun checkLoansAndDelete(book: BookModel) {
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
                        // Sin préstamos activos: proceder a eliminar
                        val dbRef = FirebaseDatabase.getInstance()
                            .getReference("libros").child(book.bookId)
                        dbRef.removeValue().addOnSuccessListener {
                            Toast.makeText(context, bookDeleteSuccess, Toast.LENGTH_SHORT).show()
                            // Borrar portada de Storage
                            if (book.imageUrl.isNotEmpty()) {
                                FirebaseStorage.getInstance().reference
                                    .child("book_covers/${book.bookId}.jpg")
                                    .delete().addOnSuccessListener {}.addOnFailureListener {}
                            }
                            // Borrar foto del autor de Storage
                            if (book.authorImageUrl.isNotEmpty()) {
                                FirebaseStorage.getInstance().reference
                                    .child("author_photos/${book.bookId}.jpg")
                                    .delete().addOnSuccessListener {}.addOnFailureListener {}
                            }
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

    // DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN
    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = {
                Text(
                    text = stringResource(id = R.string.book_dialog_delete_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(id = R.string.book_dialog_delete_body),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = " ${bookToDelete!!.title}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = YaleBlue
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val book = bookToDelete!!
                        bookToDelete = null
                        checkLoansAndDelete(book)
                    }
                ) {
                    Text(text = "ELIMINAR", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
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
                        text = "Solo se puede eliminar un libro cuando todos sus préstamos hayan sido devueltos (estatus: ENTREGADO).",
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

    // INDICADOR DE VERIFICACIÓN DE PRÉSTAMOS
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
        title = stringResource(id = R.string.book_query_title),
        isAdmin = true,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = onBackClick
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // BARRA DE BÚSQUEDA
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PremiumTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = stringResource(id = R.string.book_search_placeholder),
                            icon = Icons.Default.Search
                        )
                    }
                    IconButton(
                        onClick = { focusManager.clearFocus() },
                        modifier = Modifier
                            .size(54.dp)
                            .background(YaleBlue, shape = RoundedCornerShape(16.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.White)
                    }
                }

                // FILTRADO Y ORDENAMIENTO
                val filteredAndSortedList = remember(bookList, searchQuery) {
                    bookList
                        .filter { book ->
                            book.title.contains(searchQuery, ignoreCase = true) ||
                            book.author.contains(searchQuery, ignoreCase = true)
                        }
                        .sortedBy { it.title.trim().uppercase() }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
                } else if (filteredAndSortedList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty()) stringResource(id = R.string.book_no_registered)
                                   else stringResource(id = R.string.book_no_found),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredAndSortedList) { book ->
                            BookListItem(
                                book = book,
                                onEdit = { onEditBookClick(book) },
                                onDelete = { bookToDelete = book },
                                onClick = { onBookClick(book) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookListItem(
    book: BookModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // PORTADA
                if (book.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = book.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 46.dp, height = 60.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 60.dp)
                            .background(AirForceBlue, shape = RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Book, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = book.title.trim(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = book.author.trim(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Disponible: ${book.stock}",
                        fontSize = 12.sp,
                        color = YaleBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ACCIONES
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = YaleBlue)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935))
                }
            }
        }
    }
}