package com.example.mislibros.ui.screens.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.model.AuthorModel
import com.example.mislibros.model.BookModel
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.components.PremiumTextField
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.viewmodel.AuthViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun UserQueryAuthorsScreen(
    authViewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit,
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    onAuthorClick: (AuthorModel) -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    var bookList by remember { mutableStateOf<List<BookModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }

    // Cargar libros en tiempo real para derivar autores
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

    // Derivar lista única de autores
    val authorList = remember(bookList) {
        bookList
            .filter { it.author.trim().isNotEmpty() }
            .groupBy { it.author.trim() }
            .map { (authorName, books) ->
                val representativeBook = books.firstOrNull { it.authorBio.isNotEmpty() || it.authorImageUrl.isNotEmpty() } ?: books.first()
                AuthorModel(
                    name = authorName,
                    bio = representativeBook.authorBio,
                    imageUrl = representativeBook.authorImageUrl
                )
            }
    }

    // Filtrar autores según la búsqueda
    val filteredAuthors = remember(authorList, searchQuery) {
        authorList
            .filter { author ->
                author.name.contains(searchQuery, ignoreCase = true)
            }
            .sortedBy { it.name.uppercase() }
    }

    LibraryScaffold(
        authViewModel = authViewModel,
        title = "Buscar Autor",
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
                            label = "Buscar autor por nombre",
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

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = YaleBlue)
                    }
                } else if (filteredAuthors.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty()) "No hay autores registrados en el catálogo"
                                   else "No se encontraron autores",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredAuthors) { author ->
                            UserAuthorListItem(
                                author = author,
                                onClick = { onAuthorClick(author) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserAuthorListItem(
    author: AuthorModel,
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
                // FOTO DE PERFIL DEL AUTOR (CIRCULAR)
                if (author.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = author.imageUrl,
                        contentDescription = "Foto de ${author.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(AirForceBlue.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = YaleBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = author.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = author.bio.ifBlank { "Sin bibliografía disponible" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = YaleBlue.copy(alpha = 0.5f)
            )
        }
    }
}
