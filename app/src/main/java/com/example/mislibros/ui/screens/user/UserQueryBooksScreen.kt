package com.example.mislibros.ui.screens.user

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
import com.example.mislibros.model.BookModel
import com.example.mislibros.ui.components.PremiumTextField
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
fun UserQueryBooksScreen(
    authViewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit,
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    onLoanClick: (BookModel) -> Unit,
    onBookClick: (BookModel) -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val logoutSuccessText = stringResource(id = R.string.logout_success)
    val userProfile = authViewModel.currentUserProfile


    var bookList by remember { mutableStateOf<List<BookModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

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

    LibraryScaffold(
        authViewModel = authViewModel,
        title = stringResource(id = R.string.user_search_books_title),
        isAdmin = false,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = onBackClick
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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

                // Filtrado y ordenamiento reactivo: se recalcula de forma automatica cada vez que
                // bookList (base de datos) o searchQuery (lo que escribe el usuario) cambian.
                val filteredAndSortedList = remember(bookList, searchQuery) {
                    bookList
                        .filter { book -> 
                            book.title.contains(searchQuery, ignoreCase = true) ||
                            book.author.contains(searchQuery, ignoreCase = true)
                        }
                        .sortedBy { it.title.trim().uppercase() }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    }
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
                            UserBookListItem(
                                book = book,
                                onLoan = { onLoanClick(book) },
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
fun UserBookListItem(
    book: BookModel,
    onLoan: () -> Unit,
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
                // El peso (weight) expande este contenedor y empuja las acciones a la derecha
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

            // BOTÓN SOLICITAR PRÉSTAMO
            IconButton(onClick = onLoan) {
                Icon(
                    imageVector = Icons.Default.LibraryAdd,
                    contentDescription = "Solicitar libro",
                    tint = AmberOrange,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
