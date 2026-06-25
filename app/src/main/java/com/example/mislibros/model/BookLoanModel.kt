package com.example.mislibros.model

data class BookLoanModel(
    val loanId: String = "",
    val userId: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val telefono: String = "",
    val email: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookImageUrl: String = "",   // Portada del libro para mostrar en tarjetas
    val category: String = "",
    val loanDate: String = "",       // Fecha del préstamo / solicitud
    val durationWeeks: String = "",  // "1 semana", "2 semanas", "3 semanas"
    val deliveryDate: String = "",   // Fecha de entrega física (Solo Admin)
    val returnDate: String = "",     // Fecha límite de devolución (Solo Admin)
    val status: String = "PENDIENTE", // PENDIENTE, ACTIVO, ENTREGADO, VENCIDO
    val loanType: String = ""        // Presencial, Linea
)
