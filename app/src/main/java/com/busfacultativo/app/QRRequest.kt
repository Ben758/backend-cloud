package com.busfacultativo.app

data class QRRequest(
    val codigo_universitario: String,
    val monto: Double,
    val glosa: String
)
