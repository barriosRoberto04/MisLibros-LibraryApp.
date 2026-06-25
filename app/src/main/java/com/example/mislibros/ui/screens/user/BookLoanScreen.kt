package com.example.mislibros.ui.screens.user

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mislibros.R
import com.example.mislibros.model.BookModel
import com.example.mislibros.model.BookLoanModel
import com.example.mislibros.model.NotificationModel
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.components.PremiumDropdownField
import com.example.mislibros.ui.components.PremiumTextField
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.viewmodel.AuthViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookLoanScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onProfileClick: () -> Unit,
    preselectedBook: BookModel? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val userProfile = authViewModel.currentUserProfile
    val isAdmin = userProfile?.role == "ADMIN"

    // --- ESTADOS DEL SOLICITANTE ---
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Pre-llenar si es usuario normal
    LaunchedEffect(userProfile) {
        if (!isAdmin && userProfile != null) {
            nombre = userProfile.nombre
            apellidos = userProfile.apellidos
            telefono = userProfile.telefono
            email = userProfile.email
        }
    }

    // --- LIBROS DESDE FIREBASE ---
    var allBooks by remember { mutableStateOf<List<BookModel>>(emptyList()) }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var filteredBooks by remember { mutableStateOf<List<BookModel>>(emptyList()) }
    var isLoadingBooks by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("libros")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val booksList = mutableListOf<BookModel>()
                    val categoriesSet = mutableSetOf<String>()
                    for (child in snapshot.children) {
                        val book = child.getValue(BookModel::class.java)
                        if (book != null) {
                            booksList.add(book)
                            if (book.category.isNotBlank()) categoriesSet.add(book.category)
                        }
                    }
                    allBooks = booksList
                    categories = categoriesSet.toList().sorted()
                    isLoadingBooks = false
                }
                override fun onCancelled(error: DatabaseError) {
                    isLoadingBooks = false
                    Toast.makeText(context, "Error al cargar libros: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // --- SELECCIÓN DE LIBRO ---
    var selectedCategory by remember { mutableStateOf("-------") }
    var selectedBookTitle by remember { mutableStateOf("-------") }
    var selectedBook by remember { mutableStateOf<BookModel?>(null) }

    // Pre-seleccionar libro cuando viene desde Buscar Libros
    LaunchedEffect(allBooks, preselectedBook) {
        if (preselectedBook != null && allBooks.isNotEmpty()) {
            selectedCategory = preselectedBook.category
            filteredBooks = allBooks.filter { it.category == preselectedBook.category }
            selectedBookTitle = preselectedBook.title
            selectedBook = preselectedBook
        }
    }

    LaunchedEffect(selectedCategory) {
        // Solo limpiar la selección si el usuario cambia la categoría manualmente
        // (no cuando se pre-llenó desde un libro seleccionado)
        if (selectedCategory != "-------" && selectedBook?.category != selectedCategory) {
            filteredBooks = allBooks.filter { it.category == selectedCategory }
            selectedBookTitle = "-------"
            selectedBook = null
        } else if (selectedCategory == "-------") {
            filteredBooks = emptyList()
            selectedBookTitle = "-------"
            selectedBook = null
        }
    }

    // --- DETALLES DEL PRÉSTAMO ---
    var loanDate by remember { mutableStateOf("") }
    var durationWeeks by remember { mutableStateOf("1 semana") }
    val durationOptions = listOf("1 semana", "2 semanas", "3 semanas")

    // Solo Admin
    var deliveryDate by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var loanStatus by remember { mutableStateOf("-------") }
    val statusOptions = listOf("-------", "ACTIVO", "PENDIENTE", "ENTREGADO", "VENCIDO")
    var selectedLoanType by remember { mutableStateOf("-------") }
    val loanTypeOptions = listOf("-------", "Presencial", "Linea")

    // Nombre del prestador (administrador que registra el préstamo)
    val nombrePrestador = if (isAdmin) {
        val apels = userProfile?.apellidos ?: ""
        "${userProfile?.nombre ?: ""} $apels".trim()
    } else ""

    // --- DATE PICKERS ---
    val calendar = Calendar.getInstance()

    val loanDatePicker = DatePickerDialog(
        context,
        { _, y, m, d -> loanDate = String.format("%02d/%02d/%d", d, m + 1, y) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )
    val deliveryDatePicker = DatePickerDialog(
        context,
        { _, y, m, d -> deliveryDate = String.format("%02d/%02d/%d", d, m + 1, y) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )
    val returnDatePicker = DatePickerDialog(
        context,
        { _, y, m, d -> returnDate = String.format("%02d/%02d/%d", d, m + 1, y) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    var isSubmitting by remember { mutableStateOf(false) }

    LibraryScaffold(
        authViewModel = authViewModel,
        title = if (isAdmin) "Registrar Préstamo" else "Solicitar Libro",
        isAdmin = isAdmin,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = onBackClick
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoadingBooks) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(20.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            // ══════════════════════════════════════
                            //   SECCIÓN 1: DATOS DEL SOLICITANTE
                            // ══════════════════════════════════════
                            SectionHeader(text = "Datos del Solicitante")

                            PremiumTextField(
                                value = nombre,
                                onValueChange = { if (isAdmin) nombre = it },
                                label = "Nombre(s)",
                                icon = Icons.Default.Person,
                                readOnly = !isAdmin,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                            Spacer(Modifier.height(12.dp))

                            PremiumTextField(
                                value = apellidos,
                                onValueChange = { if (isAdmin) apellidos = it },
                                label = "Apellidos",
                                icon = Icons.Default.PersonOutline,
                                readOnly = !isAdmin,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                            Spacer(Modifier.height(12.dp))

                            PremiumTextField(
                                value = telefono,
                                onValueChange = { v ->
                                    if (isAdmin && v.length <= 10 && v.all { it.isDigit() }) telefono = v
                                },
                                label = "Teléfono Celular (10 dígitos)",
                                icon = Icons.Default.Phone,
                                readOnly = !isAdmin,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                            Spacer(Modifier.height(12.dp))

                            PremiumTextField(
                                value = email,
                                onValueChange = { if (isAdmin) email = it },
                                label = "Correo Electrónico",
                                icon = Icons.Default.Email,
                                readOnly = !isAdmin,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                            Spacer(Modifier.height(24.dp))

                            // ══════════════════════════════════════
                            //   SECCIÓN 2: SELECCIÓN DE LIBRO
                            // ══════════════════════════════════════
                            SectionHeader(text = "Libro Solicitado")

                            PremiumDropdownField(
                                value = selectedCategory,
                                label = "Género o Categoría",
                                icon = Icons.Default.Category,
                                options = listOf("-------") + categories,
                                onOptionSelected = { selectedCategory = it }
                            )
                            Spacer(Modifier.height(12.dp))

                            val bookTitlesOptions = listOf("-------") + filteredBooks.map { it.title }
                            PremiumDropdownField(
                                value = selectedBookTitle,
                                label = if (selectedCategory == "-------") "Selecciona primero un género" else "Título del Libro",
                                icon = Icons.Default.Book,
                                options = if (selectedCategory == "-------") listOf("-------") else bookTitlesOptions,
                                onOptionSelected = { title ->
                                    selectedBookTitle = title
                                    selectedBook = filteredBooks.find { it.title == title }
                                }
                            )
                            Spacer(Modifier.height(12.dp))

                            selectedBook?.let { book ->
                                val stockVal = book.stock.toIntOrNull() ?: 0
                                PremiumTextField(
                                    value = book.author,
                                    onValueChange = {},
                                    label = "Autor",
                                    icon = Icons.Default.Create,
                                    readOnly = true
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (stockVal > 0) "✓ Disponible — Stock: $stockVal unidades"
                                    else "✗ Sin ejemplares disponibles para préstamo",
                                    color = if (stockVal > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            // ══════════════════════════════════════
                            //   SECCIÓN 3: DETALLES DEL PRÉSTAMO
                            // ══════════════════════════════════════
                            SectionHeader(text = "Detalles del Préstamo")

                            // Campos exclusivos del Administrador (nombre prestador va de primero)
                            if (isAdmin) {
                                // Nombre del prestador (auto-llenado con el nombre del Admin)
                                PremiumTextField(
                                    value = nombrePrestador,
                                    onValueChange = {},
                                    label = "Nombre del Prestador",
                                    icon = Icons.Default.AdminPanelSettings,
                                    readOnly = true
                                )
                                Spacer(Modifier.height(12.dp))

                                // Tipo de Préstamo select field
                                PremiumDropdownField(
                                    value = selectedLoanType,
                                    label = "Tipo de Préstamo",
                                    icon = Icons.Default.Info,
                                    options = loanTypeOptions,
                                    onOptionSelected = { selectedLoanType = it }
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            // Fecha del préstamo
                            OutlinedTextField(
                                value = loanDate,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Fecha de Préstamo / Solicitud") },
                                leadingIcon = { Icon(Icons.Default.DateRange, null, tint = YaleBlue) },
                                trailingIcon = {
                                    IconButton(onClick = { loanDatePicker.show() }) {
                                        Icon(Icons.Default.CalendarToday, null, tint = YaleBlue)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = YaleBlue,
                                    unfocusedBorderColor = YaleBlue.copy(alpha = 0.4f)
                                )
                            )
                            Spacer(Modifier.height(12.dp))

                            PremiumDropdownField(
                                value = durationWeeks,
                                label = "Duración del Préstamo",
                                icon = Icons.Default.Timelapse,
                                options = durationOptions,
                                onOptionSelected = { durationWeeks = it }
                            )
                            Spacer(Modifier.height(12.dp))

                            // Campos exclusivos del Administrador (el resto)
                            if (isAdmin) {
                                OutlinedTextField(
                                    value = deliveryDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Fecha de Entrega Física") },
                                    leadingIcon = { Icon(Icons.Default.AssignmentReturned, null, tint = YaleBlue) },
                                    trailingIcon = {
                                        IconButton(onClick = { deliveryDatePicker.show() }) {
                                            Icon(Icons.Default.CalendarToday, null, tint = YaleBlue)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = YaleBlue,
                                        unfocusedBorderColor = YaleBlue.copy(alpha = 0.4f)
                                    )
                                )
                                Spacer(Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = returnDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Fecha de Devolución") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.AssignmentReturn, null, tint = YaleBlue) },
                                    trailingIcon = {
                                        IconButton(onClick = { returnDatePicker.show() }) {
                                            Icon(Icons.Default.CalendarToday, null, tint = YaleBlue)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = YaleBlue,
                                        unfocusedBorderColor = YaleBlue.copy(alpha = 0.4f)
                                    )
                                )
                                Spacer(Modifier.height(12.dp))

                                // Estatus: -------, ACTIVO, PENDIENTE, ENTREGADO, VENCIDO
                                PremiumDropdownField(
                                    value = loanStatus,
                                    label = "Estatus del Préstamo",
                                    icon = Icons.Default.Info,
                                    options = statusOptions,
                                    onOptionSelected = { loanStatus = it }
                                )
                                Spacer(Modifier.height(24.dp))
                            }

                            // ══════════════════════════════════════
                            //   BOTONES
                            // ══════════════════════════════════════
                            Button(
                                onClick = {
                                    val currentBook = selectedBook

                                    // ── Validaciones ──
                                    // Validar campos vacíos del solicitante
                                    if (nombre.isBlank() || apellidos.isBlank() || telefono.isBlank() || email.isBlank()) {
                                        Toast.makeText(context, "Por favor, completa todos los datos del solicitante.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Validar longitud del teléfono
                                    if (telefono.length != 10) {
                                        Toast.makeText(context, "El teléfono debe tener exactamente 10 dígitos.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Validar formato del correo
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                        Toast.makeText(context, "Ingresa un correo electrónico válido.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Validar selección de categoría y libro
                                    if (selectedCategory == "-------" || selectedBookTitle == "-------" || currentBook == null) {
                                        Toast.makeText(context, "Selecciona un género y un título de libro.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Validar disponibilidad de stock del libro
                                    val stockVal = currentBook.stock.toIntOrNull() ?: 0
                                    if (stockVal <= 0) {
                                        Toast.makeText(context, "El libro seleccionado no cuenta con ejemplares disponibles.", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    // Validar fecha del préstamo
                                    if (loanDate.isBlank()) {
                                        Toast.makeText(context, "Selecciona la fecha del préstamo.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Validar estatus del préstamo para administradores
                                    if (isAdmin && loanStatus == "-------") {
                                        Toast.makeText(context, "Selecciona el estatus del préstamo.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Validar fechas de entrega y devolución para administradores
                                    if (isAdmin && (deliveryDate.isBlank() || returnDate.isBlank())) {
                                        Toast.makeText(context, "Captura las fechas de entrega y devolución.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Validar tipo de préstamo para administradores
                                    if (isAdmin && selectedLoanType == "-------") {
                                        Toast.makeText(context, "Selecciona el tipo de préstamo.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    isSubmitting = true
                                    val loanRef = FirebaseDatabase.getInstance().getReference("prestamos")
                                    val newLoanId = loanRef.push().key ?: UUID.randomUUID().toString()

                                    val loanModel = BookLoanModel(
                                        loanId = newLoanId,
                                        userId = if (isAdmin) "" else (userProfile?.userId ?: ""),
                                        nombre = nombre.trim(),
                                        apellidos = apellidos.trim(),
                                        telefono = telefono.trim(),
                                        email = email.trim(),
                                        bookId = currentBook.bookId,
                                        bookTitle = currentBook.title,
                                        bookAuthor = currentBook.author,
                                        bookImageUrl = currentBook.imageUrl,
                                        category = currentBook.category,
                                        loanDate = loanDate,
                                        durationWeeks = durationWeeks,
                                        deliveryDate = if (isAdmin) deliveryDate else "",
                                        returnDate = if (isAdmin) returnDate else "",
                                        status = if (isAdmin) loanStatus else "PENDIENTE",
                                        loanType = if (isAdmin) selectedLoanType else "Linea"
                                    )

                                    loanRef.child(newLoanId).setValue(loanModel)
                                        .addOnSuccessListener {
                                            // Descontar stock si Admin + ACTIVO
                                            if (isAdmin && loanStatus == "ACTIVO") {
                                                FirebaseDatabase.getInstance().getReference("libros")
                                                    .child(currentBook.bookId).child("stock")
                                                    .setValue((stockVal - 1).toString())
                                            }
                                            // Notificación al Admin si es usuario normal
                                            if (!isAdmin) {
                                                val notifRef = FirebaseDatabase.getInstance().getReference("notificaciones")
                                                val notifId = notifRef.push().key ?: UUID.randomUUID().toString()
                                                val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                                                // Crear notificacion dirigida a los administradores
                                                val notification = NotificationModel(
                                                    notificationId = notifId,
                                                    title = "Prestamo: $nombre $apellidos".trim(), // Nombre del solicitante
                                                    message = "Solicita el libro '${currentBook.title}' por $durationWeeks.",
                                                    timestamp = now,
                                                    read = false,
                                                    targetRole = "ADMIN", //<-- Indica que solo la recibira el Admin
                                                    userImageUrl = userProfile?.imageUrl ?: "", // Foto de perfil del usuario
                                                    bookImageUrl = currentBook.imageUrl // Portada del libro solicitado
                                                )
                                                notifRef.child(notifId).setValue(notification)
                                            }
                                            Toast.makeText(context, "Préstamo registrado exitosamente", Toast.LENGTH_SHORT).show()
                                            isSubmitting = false
                                            onBackClick()
                                        }
                                        .addOnFailureListener { e ->
                                            isSubmitting = false
                                            Toast.makeText(context, "Error al registrar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AirForceBlue, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                enabled = !isSubmitting
                            ) {
                                Text(
                                    text = if (isAdmin) "REGISTRAR" else "ENVIAR SOLICITUD",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (isAdmin) {
                                        nombre = ""
                                        apellidos = ""
                                        telefono = ""
                                        email = ""
                                        selectedLoanType = "-------"
                                    }
                                    selectedCategory = "-------"
                                    selectedBookTitle = "-------"
                                    selectedBook = null
                                    loanDate = ""
                                    durationWeeks = "1 semana"
                                    deliveryDate = ""
                                    returnDate = ""
                                    loanStatus = "-------"
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                enabled = !isSubmitting
                            ) {
                                Text(text = stringResource(id = R.string.cancel_button), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = YaleBlue,
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
    )
}
