package com.busfacultativo.app

data class RegisterRequest(
    val nombres: String,
    val apellidos: String,
    val ci: String,
    val codigo_universitario: String,
    val correo: String,
    val password: String
)