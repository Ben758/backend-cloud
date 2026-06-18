package com.busfacultativo.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun RegistroScreen(navController: NavHostController) {

    var nombres by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }

    var ci by remember { mutableStateOf("") }
    var complemento by remember { mutableStateOf("") }

    var ru by remember { mutableStateOf("") }

    var correo by remember { mutableStateOf("") }
    var celular by remember { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Registro de Usuario",
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        OutlinedTextField(
            value = nombres,
            onValueChange = { nombres = it },
            label = { Text("Nombre(s)") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = apellidoPaterno,
            onValueChange = { apellidoPaterno = it },
            label = { Text("Apellido Paterno") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = apellidoMaterno,
            onValueChange = { apellidoMaterno = it },
            label = { Text("Apellido Materno") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = ci,
            onValueChange = { ci = it },
            label = { Text("Carnet de Identidad") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = complemento,
            onValueChange = { complemento = it },
            label = { Text("Complemento (Opcional)") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = ru,
            onValueChange = { ru = it },
            label = { Text("RU") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo Electrónico") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = celular,
            onValueChange = { celular = it },
            label = { Text("Celular") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = confirmarPassword,
            onValueChange = { confirmarPassword = it },
            label = { Text("Confirmar Contraseña") }
        )

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {

            }
        ) {
            Text("Crear Cuenta")
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}