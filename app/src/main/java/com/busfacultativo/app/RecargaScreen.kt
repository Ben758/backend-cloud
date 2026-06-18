package com.busfacultativo.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

@Composable
fun RecargaScreen() {
    val context = LocalContext.current
    val usuario = SessionManager.usuario

    var ru by remember { mutableStateOf(usuario?.codigo_universitario ?: "") }
    var cantidadText by remember { mutableStateOf("") }
    var qrUrl by remember { mutableStateOf<String?>(null) }
    var transaccionId by remember { mutableStateOf<String?>(null) }
    var cargandoQR by remember { mutableStateOf(false) }

    val PRECIO_POR_VIAJE = 1.50
    val cantidad = cantidadText.toIntOrNull() ?: 0
    val totalBOB = cantidad * PRECIO_POR_VIAJE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tarjeta de Entrada
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Detalles de Recarga",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = ru,
                    onValueChange = { ru = it },
                    label = { Text("RU (Código Universitario)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = cantidadText,
                    onValueChange = { cantidadText = it },
                    label = { Text("Cantidad de viajes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjeta de Monto y Acción
        if (cantidad > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Total a pagar", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", totalBOB)} BOB",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Button(
                        onClick = {
                            if (cantidad <= 0) return@Button
                            cargandoQR = true
                            val request = QRRequest(
                                codigo_universitario = ru,
                                monto = totalBOB,
                                glosa = "Recarga de $cantidad viajes"
                            )
                            RetrofitClient.apiService.generarQR(request).enqueue(object : Callback<QRResponse> {
                                override fun onResponse(call: Call<QRResponse>, response: Response<QRResponse>) {
                                    cargandoQR = false
                                    if (response.isSuccessful && response.body()?.ok == true) {
                                        qrUrl = response.body()?.qr_image_url
                                        transaccionId = response.body()?.transaccion_id
                                    } else {
                                        Toast.makeText(context, "Error al generar QR", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                override fun onFailure(call: Call<QRResponse>, t: Throwable) {
                                    cargandoQR = false
                                    Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                                }
                            })
                        },
                        enabled = !cargandoQR,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (cargandoQR) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Pagar con QR")
                        }
                    }
                }
            }
        }

        qrUrl?.let { url ->
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(text = "Escanea el código QR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.size(280.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(model = url, contentDescription = "QR", modifier = Modifier.size(240.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    transaccionId?.let { tid ->
                        verificarPagoLocal(context, tid, ru, cantidad)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Ya realicé el pago", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

private fun verificarPagoLocal(context: android.content.Context, transaccionId: String, ru: String, cantidad: Int) {
    RetrofitClient.apiService.verificarPago(VerificarPagoRequest(transaccionId))
        .enqueue(object : Callback<VerificarPagoResponse> {
            override fun onResponse(call: Call<VerificarPagoResponse>, response: Response<VerificarPagoResponse>) {
                if (response.isSuccessful && response.body()?.ok == true) {
                    ejecutarRecargaLocal(context, ru, cantidad)
                } else {
                    Toast.makeText(context, "Pago no confirmado aún", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<VerificarPagoResponse>, t: Throwable) {
                Toast.makeText(context, "Error al verificar pago", Toast.LENGTH_SHORT).show()
            }
        })
}

private fun ejecutarRecargaLocal(context: android.content.Context, ru: String, cantidad: Int) {
    RetrofitClient.apiService.recargar(RecargaRequest(ru, cantidad))
        .enqueue(object : Callback<RecargaResponse> {
            override fun onResponse(call: Call<RecargaResponse>, response: Response<RecargaResponse>) {
                if (response.isSuccessful && response.body()?.ok == true) {
                    Toast.makeText(context, "¡Recarga exitosa! Saldo: ${response.body()?.saldo_actual}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<RecargaResponse>, t: Throwable) {
                Toast.makeText(context, "Error al procesar recarga", Toast.LENGTH_SHORT).show()
            }
        })
}
