package com.example.mislibros.ui.screens.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.model.AuthorModel
import com.example.mislibros.model.BookModel
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.viewmodel.AuthViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun AuthorDetailScreen(
    authViewModel: AuthViewModel,
    author: AuthorModel?,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onProfileClick: () -> Unit,
    onBookClick: (BookModel) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    var authorBooks by remember { mutableStateOf<List<BookModel>>(emptyList()) }
    var isLoadingBooks by remember { mutableStateOf(true) }

    // Carga dinamica y en tiempo real de los libros que han sido escritos por el autor actual
    LaunchedEffect(author) {
        if (author == null) return@LaunchedEffect
        val database = FirebaseDatabase.getInstance().getReference("libros")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BookModel>()
                for (child in snapshot.children) {
                    val book = child.getValue(BookModel::class.java)
                    // Filtrar comparando el nombre del autor de forma insensible a mayusculas/minusculas
                    if (book != null && book.author.trim().equals(author.name.trim(), ignoreCase = true)) {
                        list.add(book)
                    }
                }
                authorBooks = list // Almacenar en la lista reactiva para mostrar en el carrusel horizontal
                isLoadingBooks = false
            }
            override fun onCancelled(error: DatabaseError) {
                isLoadingBooks = false
                Toast.makeText(context, "Error al cargar libros: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        database.addValueEventListener(listener)
    }

    LibraryScaffold(
        authViewModel = authViewModel,
        title = "Detalle del Autor",
        isAdmin = false,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = onBackClick
    ) { innerPadding ->
        if (author == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se pudo cargar la información del autor",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
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
                    Spacer(modifier = Modifier.height(24.dp))

                    // FOTO CIRCULAR DEL AUTOR GRANDE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .background(YaleBlue.copy(alpha = 0.08f), shape = CircleShape)
                                .border(3.dp, YaleBlue, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (author.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = author.imageUrl,
                                    contentDescription = "Foto de ${author.name}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = YaleBlue,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // NOMBRE DEL AUTOR
                        Text(
                            text = author.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = YaleBlue,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Autor principal",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // SECCIÓN BIBLIOGRAFÍA
                    Text(
                        text = "Bibliografía",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = YaleBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 28.dp)
                    ) {
                        Text(
                            text = author.bio.ifBlank { "No hay bibliografía disponible para este autor." },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // SECCIÓN LIBROS DE ESTE AUTOR
                    Text(
                        text = "Libros de este Autor",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = YaleBlue,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (isLoadingBooks) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = YaleBlue)
                        }
                    } else if (authorBooks.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No se encontraron libros de este autor en el catálogo.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            items(authorBooks) { book ->
                                AuthorBookItem(book = book, onClick = { onBookClick(book) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorBookItem(
    book: BookModel,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Portada
            if (book.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = book.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 94.dp, height = 120.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 94.dp, height = 120.dp)
                        .background(AirForceBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = YaleBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Título
            Text(
                text = book.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Stock: ${book.stock}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = YaleBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
