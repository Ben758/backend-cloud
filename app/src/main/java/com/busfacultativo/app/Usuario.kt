package com.busfacultativo.app

data class Usuario(
    val uid: String,
    val usuario_id: String,
    val nombre: String,
    val codigo_universitario: String,
    val ci: String,
    val carrera: String,
    val saldo_viajes: Int
)