package com.example.mislibros.model

data class NotificationModel(
    val notificationId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: String = "",
    val read: Boolean = false,
    val targetRole: String = "ADMIN",
    val targetUserId: String = "",
    val userImageUrl: String = "",
    val bookImageUrl: String = ""
)
