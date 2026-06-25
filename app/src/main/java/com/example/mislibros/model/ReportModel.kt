package com.example.mislibros.model

data class ReportModel(
    val reportId: String = "",
    val userId: String = "",              // Vacío si fue reportado de forma presencial por Admin para un externo
    val reporterName: String = "",
    val reporterApellidos: String = "",
    val reporterEmail: String = "",
    val reporterPhone: String = "",
    val reporterImageUrl: String = "",     // Foto del usuario si es del sistema
    val assignedAdmin: String = "",        // Nombre del administrador que registró (si aplica)
    val subject: String = "",
    val description: String = "",
    val status: String = "-------",        // -------, En Proceso, Resuelto, Finalizado
    val timestamp: String = "",
    val reportType: String = "Linea",      // -------, Presencial, Telefónico, Linea
    val reportDate: String = ""            // Fecha seleccionada manualmente al subir el reporte
)
