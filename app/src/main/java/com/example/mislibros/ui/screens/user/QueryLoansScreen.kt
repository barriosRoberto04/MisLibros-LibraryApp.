package com.example.mislibros.ui.screens.user

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.R
import com.example.mislibros.model.BookModel
import com.example.mislibros.model.BookLoanModel
import com.example.mislibros.model.NotificationModel
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.components.PremiumDropdownField
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.viewmodel.AuthViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

@Composable
fun QueryLoansScreen(
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

    // ESTADOS DE PRÉSTAMOS
    var loansList by remember { mutableStateOf<List<BookLoanModel>>(emptyList()) }
    var booksMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var filterStatus by remember { mutableStateOf("PENDIENTE") } // PENDIENTE, ACTIVO, ENTREGADO, VENCIDO

    // Cargar préstamos en tiempo real
    LaunchedEffect(Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("prestamos")
        val booksRef = FirebaseDatabase.getInstance().getReference("libros")

        // Escuchar cambios en libros para tener portadas en tiempo real
        val booksListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, String>()
                for (child in snapshot.children) {
                    val bId = child.child("bookId").getValue(String::class.java) ?: ""
                    val imgUrl = child.child("imageUrl").getValue(String::class.java) ?: ""
                    if (bId.isNotEmpty()) {
                        map[bId] = imgUrl
                    }
                }
                booksMap = map
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        booksRef.addValueEventListener(booksListener)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BookLoanModel>()
                for (child in snapshot.children) {
                    val loan = child.getValue(BookLoanModel::class.java)
                    if (loan != null) {
                        // Si es usuario normal, solo mostrar sus propios préstamos
                        if (isAdmin || loan.userId == currentUserId) {
                            list.add(loan)
                        }
                    }
                }
                loansList = list
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                Toast.makeText(context, "Error al cargar préstamos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        databaseRef.addValueEventListener(listener)
    }

    // DIÁLOGO DE EDICIÓN PARA ADMIN
    var selectedLoanToEdit by remember { mutableStateOf<BookLoanModel?>(null) }
    var newStatus by remember { mutableStateOf("PENDIENTE") }
    var newDeliveryDate by remember { mutableStateOf("") }
    var newReturnDate by remember { mutableStateOf("") }
    var isSavingEdit by remember { mutableStateOf(false) }

    LaunchedEffect(selectedLoanToEdit) {
        selectedLoanToEdit?.let { loan ->
            newStatus = loan.status
            newDeliveryDate = loan.deliveryDate
            newReturnDate = loan.returnDate
        }
    }

    val calendar = Calendar.getInstance()

    val deliveryDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            newDeliveryDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val returnDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            newReturnDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (selectedLoanToEdit != null) {
        val loan = selectedLoanToEdit!!
        AlertDialog(
            onDismissRequest = { if (!isSavingEdit) selectedLoanToEdit = null },
            title = {
                Text(
                    text = "Editar Estatus de Préstamo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = YaleBlue
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Libro: ${loan.bookTitle}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Solicitante: ${"${loan.nombre} ${loan.apellidos}".trim()}", fontSize = 13.sp)

                    // Estatus Dropdown
                    PremiumDropdownField(
                        value = newStatus,
                        label = "Estatus",
                        icon = Icons.Default.Info,
                        options = listOf("PENDIENTE", "ACTIVO", "ENTREGADO", "VENCIDO"),
                        onOptionSelected = { newStatus = it }
                    )

                    // Fecha de Entrega
                    OutlinedTextField(
                        value = newDeliveryDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha de Entrega Física") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = YaleBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            IconButton(onClick = { deliveryDatePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = YaleBlue)
                            }
                        }
                    )

                    // Fecha de Devolución
                    OutlinedTextField(
                        value = newReturnDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha de Devolución") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = YaleBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            IconButton(onClick = { returnDatePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = YaleBlue)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSavingEdit = true
                        
                        // Referencias para actualizar el préstamo y descontar/devolver stock en libros
                        val prestamosRef = FirebaseDatabase.getInstance().getReference("prestamos").child(loan.loanId)
                        val librosRef = FirebaseDatabase.getInstance().getReference("libros").child(loan.bookId)
                        
                        // Leer stock actual del libro antes de actualizar el préstamo
                        librosRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val book = snapshot.getValue(BookModel::class.java)
                                val currentStock = try { book?.stock?.toInt() ?: 0 } catch(e: Exception) { 0 }
                                
                                // Determinar si restamos stock (-1 al activar) o sumamos (+1 al entregar)
                                val stockDiff: Int = when {
                                    loan.status != "ACTIVO" && newStatus == "ACTIVO" -> {
                                        if (currentStock <= 0) {
                                            Toast.makeText(context, "No hay stock suficiente para activar este préstamo.", Toast.LENGTH_LONG).show()
                                            isSavingEdit = false
                                            return
                                        }
                                        -1
                                    }
                                    loan.status == "ACTIVO" && newStatus == "ENTREGADO" -> 1
                                    else -> 0
                                }

                                val updatedLoan = loan.copy(
                                    status = newStatus,
                                    deliveryDate = newDeliveryDate,
                                    returnDate = newReturnDate,
                                    bookImageUrl = book?.imageUrl ?: loan.bookImageUrl
                                )

                                // Guardar cambios en el préstamo
                                prestamosRef.setValue(updatedLoan)
                                    .addOnSuccessListener {
                                        // Actualizar el stock del libro si hubo cambio
                                        if (stockDiff != 0) {
                                            val finalStock = (currentStock + stockDiff).toString()
                                            librosRef.child("stock").setValue(finalStock)
                                        }
                                        // Crear notificacion dirigida al usuario normal si su estatus cambio
                                        if (loan.userId.isNotBlank() && loan.status != newStatus) {
                                            val notifRef = FirebaseDatabase.getInstance().getReference("notificaciones")
                                            val notifId = notifRef.push().key ?: UUID.randomUUID().toString()
                                            val nowStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                            val statusTitle = newStatus.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                                            val notification = NotificationModel(
                                                notificationId = notifId,
                                                title = "Prestamo $statusTitle",
                                                message = loan.bookTitle,
                                                timestamp = nowStr,
                                                read = false,
                                                targetRole = "USER",
                                                targetUserId = loan.userId,
                                                bookImageUrl = book?.imageUrl ?: ""
                                            )
                                            notifRef.child(notifId).setValue(notification)
                                        }
                                        Toast.makeText(context, "Préstamo actualizado exitosamente", Toast.LENGTH_SHORT).show()
                                        isSavingEdit = false
                                        selectedLoanToEdit = null
                                    }
                                    .addOnFailureListener { e ->
                                        isSavingEdit = false
                                        Toast.makeText(context, "Error al actualizar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                isSavingEdit = false
                                Toast.makeText(context, "Error en Base de Datos: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AirForceBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("GUARDAR", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedLoanToEdit = null },
                    enabled = !isSavingEdit
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    LibraryScaffold(
        authViewModel = authViewModel,
        title = if (isAdmin) "Consultar Préstamos" else "Mis Préstamos",
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
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // FILTROS DE PRÉSTAMOS (Chip selector)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val states = listOf("PENDIENTE", "ACTIVO", "ENTREGADO", "VENCIDO")
                states.forEach { state ->
                    val isSelected = filterStatus == state
                    FilterChip(
                        selected = isSelected,
                        onClick = { filterStatus = state },
                        label = { Text(state, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberOrange,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // LISTADO DE PRÉSTAMOS
            val displayList = loansList.filter {
                it.status == filterStatus
            }.sortedByDescending { it.loanDate }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                }
            } else if (displayList.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No se encontraron préstamos registrados",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList) { loan ->
                        val resolvedImageUrl = booksMap[loan.bookId] ?: loan.bookImageUrl
                        LoanCard(
                            loan = loan,
                            bookImageUrl = resolvedImageUrl,
                            isAdmin = isAdmin
                        ) {
                            if (isAdmin) {
                                selectedLoanToEdit = loan
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LoanCard(
    loan: BookLoanModel,
    bookImageUrl: String,
    isAdmin: Boolean,
    onClick: () -> Unit
) {
    val badgeColor = when (loan.status) {
        "PENDIENTE" -> AmberOrange
        "ACTIVO" -> Color(0xFF4CAF50)
        "ENTREGADO" -> Color(0xFF9E9E9E)
        "VENCIDO" -> Color(0xFFE53935)
        else -> YaleBlue
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Portada del libro a la izquierda
            if (isAdmin && loan.loanType == "Presencial") {
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 60.dp)
                        .background(AmberOrange.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentReturned,
                        contentDescription = "Préstamo Presencial",
                        tint = AmberOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (bookImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = bookImageUrl,
                    contentDescription = "Portada del libro",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 46.dp, height = 60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 60.dp)
                        .background(AirForceBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = AirForceBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = loan.bookTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = YaleBlue,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = loan.status,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Autor: ${loan.bookAuthor}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Solicitante: ${"${loan.nombre} ${loan.apellidos}".trim()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Contacto: ${loan.telefono} | ${loan.email}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (loan.loanType.isNotBlank()) {
                        Text(
                            text = "Tipo de Préstamo: ${loan.loanType}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = YaleBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Fecha Préstamo", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Text(text = loan.loanDate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Duración", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Text(text = loan.durationWeeks, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                if (loan.deliveryDate.isNotBlank() || loan.returnDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (loan.deliveryDate.isNotBlank()) {
                            Column {
                                Text(text = "Entregado", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                Text(text = loan.deliveryDate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (loan.returnDate.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Devolución Límite", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                Text(text = loan.returnDate, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (loan.status == "VENCIDO") Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}
