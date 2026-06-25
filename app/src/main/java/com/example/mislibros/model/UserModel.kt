package com.example.mislibros.model

data class UserModel(
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val role: String = "USER",
    val status: String = "ACTIVE",
    val imageUrl: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val telefono: String = "",
    val calle: String = "",
    val noInterior: String = "",
    val noExterior: String = "",
    val colonia: String = "",
    val alcaldia: String = "",
    val codigoPostal: String = "",
    val fechaNacimiento: String = "",
    val fechaRegistro: String = "",
    val password: String = ""
)