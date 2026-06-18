package com.busfacultativo.app

data class QRResponse(
    val ok: Boolean,
    val qr_image_url: String?,
    val transaccion_id: String?
)
