package com.example.mislibros.ui.screens.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.mislibros.model.ReportModel
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QueryReportsScreen(
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

    // ESTADOS DE REPORTES
    var reportsList by remember { mutableStateOf<List<ReportModel>>(emptyList()) }
    var filterStatus by remember { mutableStateOf("SOLICITUD") }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar reportes en tiempo real
    LaunchedEffect(Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("reportes")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ReportModel>()
                for (child in snapshot.children) {
                    val report = child.getValue(ReportModel::class.java)
                    if (report != null) {
                        // Si es Admin, ve todos los reportes. Si es User, solo sus propios reportes
                        if (isAdmin || report.userId == currentUserId) {
                            list.add(report)
                        }
                    }
                }
                reportsList = list.sortedByDescending { it.timestamp }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                Toast.makeText(context, "Error al cargar reportes: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        databaseRef.addValueEventListener(listener)
    }

    // DIÁLOGOS DE DETALLE / EDICIÓN
    var selectedReport by remember { mutableStateOf<ReportModel?>(null) }
    var newStatus by remember { mutableStateOf("-------") }
    var isSavingStatus by remember { mutableStateOf(false) }

    LaunchedEffect(selectedReport) {
        selectedReport?.let { report ->
            newStatus = report.status
        }
    }

    if (selectedReport != null) {
        val report = selectedReport!!
        AlertDialog(
            onDismissRequest = { if (!isSavingStatus) selectedReport = null },
            title = {
                Text(
                    text = stringResource(id = R.string.report_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = YaleBlue
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(text = "Asunto: ${report.subject}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = YaleBlue)
                    Text(text = "Fecha: ${report.reportDate.ifBlank { report.timestamp }}", fontSize = 11.sp, color = Color.Gray)
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text(text = "Reportante: ${report.reporterName} ${report.reporterApellidos}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(text = "Contacto: ${report.reporterPhone} | ${report.reporterEmail}", fontSize = 12.sp, color = Color.Gray)
                    
                    if (report.assignedAdmin.isNotBlank()) {
                        Text(text = "Encargado de Registro: ${report.assignedAdmin}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(text = "Tipo de Reporte: ${report.reportType}", fontSize = 12.sp, fontWeight = FontWeight.Medium)

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text(text = "Descripción:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(text = report.description, fontSize = 13.sp)

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    if (isAdmin) {
                        // Selección de estatus para Administrador
                        PremiumDropdownField(
                            value = newStatus,
                            label = "Estatus del Reporte",
                            icon = Icons.Default.Info,
                            options = listOf("Solicitud", "En Proceso", "Finalizado"),
                            onOptionSelected = { newStatus = it }
                        )
                    } else {
                        // Vista de estatus para usuario normal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "Estatus actual:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            ReportStatusBadge(status = report.status)
                        }
                    }
                }
            },
            confirmButton = {
                if (isAdmin) {
                    Button(
                        onClick = {
                            isSavingStatus = true
                            // Actualizar el estatus del reporte en la base de datos
                            val reportRef = FirebaseDatabase.getInstance().getReference("reportes").child(report.reportId)
                            reportRef.child("status").setValue(newStatus)
                                .addOnSuccessListener {
                                    // Crear una notificacion para el usuario si su reporte cambio de estatus
                                    if (report.userId.isNotBlank() && report.status != newStatus) {
                                        val notifRef = FirebaseDatabase.getInstance().getReference("notificaciones")
                                        val notifId = notifRef.push().key ?: UUID.randomUUID().toString()
                                        val now = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                        
                                        val notification = NotificationModel(
                                            notificationId = notifId,
                                            title = "Estatus: ${report.subject}",
                                            message = "Tu reporte cambió a: $newStatus",
                                            timestamp = now,
                                            read = false,
                                            targetRole = "USER",
                                            targetUserId = report.userId
                                        )
                                        notifRef.child(notifId).setValue(notification)
                                    }

                                    Toast.makeText(context, "Estatus actualizado correctamente", Toast.LENGTH_SHORT).show()
                                    isSavingStatus = false
                                    selectedReport = null
                                }
                                .addOnFailureListener { e ->
                                    isSavingStatus = false
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AirForceBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("GUARDAR", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { selectedReport = null },
                        colors = ButtonDefaults.buttonColors(containerColor = YaleBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CERRAR", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (isAdmin) {
                    TextButton(
                        onClick = { selectedReport = null },
                        enabled = !isSavingStatus
                    ) {
                        Text("CANCELAR", fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    LibraryScaffold(
        authViewModel = authViewModel,
        title = stringResource(id = R.string.report_query_title),
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
            Spacer(modifier = Modifier.height(16.dp))

            // FILTROS DE REPORTES (Chip selector)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val states = listOf("SOLICITUD", "EN PROCESO", "FINALIZADO")
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

            val displayList = reportsList.filter {
                val reportStatusUpper = it.status.trim().uppercase()
                when (filterStatus) {
                    "SOLICITUD" -> reportStatusUpper == "SOLICITUD" || reportStatusUpper == "-------"
                    "EN PROCESO" -> reportStatusUpper == "EN PROCESO"
                    "FINALIZADO" -> reportStatusUpper == "FINALIZADO"
                    else -> true
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                }
            } else if (reportsList.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(id = R.string.report_no_registered),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (displayList.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No se encontraron reportes con este estatus",
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
                    items(displayList) { report ->
                        ReportCard(report = report, isAdmin = isAdmin) {
                            selectedReport = report
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ReportCard(
    report: ReportModel,
    isAdmin: Boolean,
    onClick: () -> Unit
) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono / Imagen a la izquierda
            if (report.reportType.equals("Linea", ignoreCase = true) && report.reporterImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = report.reporterImageUrl,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                )
            } else {
                val isPhysical = report.reportType.equals("Presencial", ignoreCase = true) ||
                    report.reportType.equals("Telefónico", ignoreCase = true) ||
                    report.reportType.equals("Telefonico", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (isPhysical) AmberOrange.copy(alpha = 0.15f)
                            else AirForceBlue.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = null,
                        tint = if (isPhysical) AmberOrange else AirForceBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.subject,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = YaleBlue,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reportado por: ${report.reporterName} ${report.reporterApellidos}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                if (report.assignedAdmin.isNotEmpty()) {
                    Text(
                        text = "Encargado: ${report.assignedAdmin}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = report.timestamp,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun ReportStatusBadge(status: String) {
    Box(
        modifier = Modifier
            .background(AmberOrange.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            color = AmberOrange,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}
