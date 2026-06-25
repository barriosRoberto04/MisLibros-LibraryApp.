package com.example.mislibros.model

data class BookModel(
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val publisher: String = "", // Editorial
    val year: String = "",
    val edition: String = "",
    val category: String = "",
    val language: String = "",
    val pages: String = "",
    val stock: String = "1",   // Cantidad de ejemplares disponibles
    val imageUrl: String = "",
    val status: String = "AVAILABLE", // AVAILABLE, LOANED, MAINTENANCE
    val dateRegistered: String = "",
    val review: String = "",          // Reseña del libro
    val authorBio: String = "",       // Mini biografía del autor
    val authorImageUrl: String = ""   // Foto del autor
)