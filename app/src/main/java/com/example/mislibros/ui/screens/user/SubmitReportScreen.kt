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
import com.example.mislibros.model.ReportModel
import com.example.mislibros.model.NotificationModel
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.components.PremiumTextField
import com.example.mislibros.ui.components.PremiumDropdownField
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.viewmodel.AuthViewModel
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SubmitReportScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val userProfile = authViewModel.currentUserProfile
    val isAdmin = userProfile?.role == "ADMIN"

    // ESTADOS DEL REPORTANTE
    var reporterName by remember { mutableStateOf("") }
    var reporterApellidos by remember { mutableStateOf("") }
    var reporterEmail by remember { mutableStateOf("") }
    var reporterPhone by remember { mutableStateOf("") }

    // ESTADOS DEL REPORTE
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var reportStatus by remember { mutableStateOf("Solicitud") }
    var reportType by remember { mutableStateOf(if (isAdmin) "-------" else "Linea") }
    var reportDate by remember { mutableStateOf("") }

    // Pre-llenar si es usuario normal
    LaunchedEffect(userProfile) {
        if (!isAdmin && userProfile != null) {
            reporterName = userProfile.nombre
            reporterApellidos = userProfile.apellidos
            reporterEmail = userProfile.email
            reporterPhone = userProfile.telefono
        }
    }

    val nombrePrestador = if (isAdmin && userProfile != null) {
        "${userProfile.nombre} ${userProfile.apellidos}".trim()
    } else ""

    var isSubmitting by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            reportDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LibraryScaffold(
        authViewModel = authViewModel,
        title = if (isAdmin) "Subir Reporte" else "Reportar",
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // ✉️ Encabezado Estilo Correo
                        SectionHeader(text = if (isAdmin) "Datos" else "Estructura del Reporte")

                        if (isAdmin) {
                            // Nombre del Administrador encargado del registro
                            PremiumTextField(
                                value = nombrePrestador,
                                onValueChange = {},
                                label = "Encargado (Administrador)",
                                icon = Icons.Default.AdminPanelSettings,
                                readOnly = true
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        // Nombre del que va a presentar el reporte
                        PremiumTextField(
                            value = reporterName,
                            onValueChange = { v ->
                                if (isAdmin) {
                                    if (v.all { it.isLetter() || it.isWhitespace() }) reporterName = v
                                }
                            },
                            label = "Nombre del Reportante",
                            icon = Icons.Default.Person,
                            readOnly = !isAdmin,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(Modifier.height(12.dp))

                        // Apellidos
                        PremiumTextField(
                            value = reporterApellidos,
                            onValueChange = { v ->
                                if (isAdmin) {
                                    if (v.all { it.isLetter() || it.isWhitespace() }) reporterApellidos = v
                                }
                            },
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

                        // Correo Electrónico
                        PremiumTextField(
                            value = reporterEmail,
                            onValueChange = { if (isAdmin) reporterEmail = it },
                            label = "Correo Electrónico",
                            icon = Icons.Default.Email,
                            readOnly = !isAdmin,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(Modifier.height(12.dp))

                        // Teléfono Celular
                        PremiumTextField(
                            value = reporterPhone,
                            onValueChange = { v ->
                                if (isAdmin && v.length <= 10 && v.all { it.isDigit() }) reporterPhone = v
                            },
                            label = "Teléfono Celular",
                            icon = Icons.Default.Phone,
                            readOnly = !isAdmin,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        if (isAdmin) {
                            Spacer(Modifier.height(12.dp))
                            PremiumDropdownField(
                                value = reportStatus,
                                label = "Estado",
                                icon = Icons.Default.Info,
                                options = listOf("Solicitud", "En Proceso", "Finalizado"),
                                onOptionSelected = { reportStatus = it }
                            )
                            Spacer(Modifier.height(12.dp))
                            PremiumDropdownField(
                                value = reportType,
                                label = "Tipo de reporte",
                                icon = Icons.Default.Category,
                                options = listOf("-------", "Presencial", "Telefónico", "Linea"),
                                onOptionSelected = { reportType = it }
                            )
                        }
                        Spacer(Modifier.height(24.dp))

                        //  Contenido del Correo / Reporte
                        SectionHeader(text = "Detalles del Mensaje")

                        // Fecha del Reporte (va antes del Asunto)
                        OutlinedTextField(
                            value = reportDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha del Reporte", color = YaleBlue) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = YaleBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha", tint = YaleBlue)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YaleBlue,
                                unfocusedBorderColor = YaleBlue.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.height(12.dp))

                        // Asunto (Texto en negrita para asunto)
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Asunto", fontWeight = FontWeight.Bold, color = YaleBlue) },
                            leadingIcon = { Icon(Icons.Default.Subject, null, tint = YaleBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YaleBlue,
                                unfocusedBorderColor = YaleBlue.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.height(12.dp))

                        // Descripción
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción del problema") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            shape = RoundedCornerShape(16.dp),
                            maxLines = 10,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YaleBlue,
                                unfocusedBorderColor = YaleBlue.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.height(28.dp))


                        // BOTONES
                        Button(
                            onClick = {
                                // ── Validaciones ──
                                if (reporterName.isBlank() || reporterApellidos.isBlank() || reporterEmail.isBlank() || reporterPhone.isBlank() || subject.isBlank() || description.isBlank()) {
                                    Toast.makeText(context, context.getString(R.string.report_fields_error), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (reporterPhone.length != 10) {
                                    Toast.makeText(context, context.getString(R.string.report_phone_error), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(reporterEmail).matches()) {
                                    Toast.makeText(context, context.getString(R.string.report_email_error), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSubmitting = true
                                val reportsRef = FirebaseDatabase.getInstance().getReference("reportes")
                                val newReportId = reportsRef.push().key ?: UUID.randomUUID().toString()

                                val reportModel = ReportModel(
                                    reportId = newReportId,
                                    userId = if (isAdmin) "" else (userProfile?.userId ?: ""),
                                    reporterName = reporterName.trim(),
                                    reporterApellidos = reporterApellidos.trim(),
                                    reporterEmail = reporterEmail.trim(),
                                    reporterPhone = reporterPhone.trim(),
                                    reporterImageUrl = if (isAdmin) "" else (userProfile?.imageUrl ?: ""),
                                    assignedAdmin = if (isAdmin) nombrePrestador else "",
                                    subject = subject.trim(),
                                    description = description.trim(),
                                    status = if (isAdmin) reportStatus else "Solicitud",
                                    timestamp = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                                    reportType = if (isAdmin) reportType else "Linea",
                                    reportDate = reportDate.ifBlank { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
                                )

                                reportsRef.child(newReportId).setValue(reportModel)
                                    .addOnSuccessListener {
                                        // Generar notificación a administradores si lo subió un usuario normal
                                        if (!isAdmin) {
                                            val notifRef = FirebaseDatabase.getInstance().getReference("notificaciones")
                                            val notifId = notifRef.push().key ?: UUID.randomUUID().toString()
                                            val now = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                            
                                            val notification = NotificationModel(
                                                notificationId = notifId,
                                                title = "Reporte: ${userProfile?.nombre} ${userProfile?.apellidos}".trim(),
                                                message = subject.trim(),
                                                timestamp = now,
                                                read = false,
                                                targetRole = "ADMIN",
                                                userImageUrl = userProfile?.imageUrl ?: ""
                                            )
                                            notifRef.child(notifId).setValue(notification)
                                        }

                                        Toast.makeText(context, context.getString(R.string.report_submitted_success), Toast.LENGTH_SHORT).show()
                                        isSubmitting = false
                                        onBackClick()
                                    }
                                    .addOnFailureListener { e ->
                                        isSubmitting = false
                                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AirForceBlue, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !isSubmitting
                        ) {
                            Text(
                                text = stringResource(id = R.string.report_submit_button),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Botón Cancelar (ahora limpia y se queda en la pantalla)
                        Button(
                            onClick = {
                                if (isAdmin) {
                                    reporterName = ""
                                    reporterApellidos = ""
                                    reporterEmail = ""
                                    reporterPhone = ""
                                    reportStatus = "-------"
                                    reportType = "-------"
                                }
                                subject = ""
                                description = ""
                                reportDate = ""
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = YaleBlue,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    )
}
