package com.busfacultativo.app

data class UsuarioResponse(
    val id: String,
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val codigo_universitario: String,
    val saldo_viajes: Int
)