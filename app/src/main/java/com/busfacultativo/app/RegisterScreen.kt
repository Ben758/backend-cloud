package com.busfacultativo.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun PantallaRegistro(navController: NavHostController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var ci by remember { mutableStateOf("") }
    var ru by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Ingresa tus datos universitarios",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Formulario
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                RegisterField(value = nombres, onValueChange = { nombres = it }, label = "Nombres", icon = Icons.Default.Person)
                Spacer(modifier = Modifier.height(16.dp))
                RegisterField(value = apellidos, onValueChange = { apellidos = it }, label = "Apellidos", icon = Icons.Default.People)
                Spacer(modifier = Modifier.height(16.dp))
                RegisterField(value = ci, onValueChange = { ci = it }, label = "Carnet de Identidad", icon = Icons.Default.Badge)
                Spacer(modifier = Modifier.height(16.dp))
                RegisterField(value = ru, onValueChange = { ru = it }, label = "Registro Universitario (RU)", icon = Icons.Default.Fingerprint)
                Spacer(modifier = Modifier.height(16.dp))
                RegisterField(value = correo, onValueChange = { correo = it }, label = "Correo Electrónico", icon = Icons.Default.Email)
                Spacer(modifier = Modifier.height(16.dp))
                RegisterField(
                    value = password, 
                    onValueChange = { password = it }, 
                    label = "Contraseña", 
                    icon = Icons.Default.Lock,
                    isPassword = true
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                cargando = true
                val request = RegisterRequest(
                    nombres = nombres,
                    apellidos = apellidos,
                    ci = ci,
                    codigo_universitario = ru,
                    correo = correo,
                    password = password
                )

                RetrofitClient.apiService.registrarUsuario(request).enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                        cargando = false
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Registro exitoso", Toast.LENGTH_LONG).show()
                            navController.navigate("login") {
                                popUpTo("registro") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Error: Revisa tus datos", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        cargando = false
                        Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                })
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !cargando
        ) {
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Registrarme ahora", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.popBackStack() }) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RegisterField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true
    )
}
