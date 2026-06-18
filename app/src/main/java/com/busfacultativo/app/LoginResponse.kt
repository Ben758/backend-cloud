package com.busfacultativo.app

data class LoginResponse(
    val ok: Boolean,
    val usuario: UsuarioLogin?
)