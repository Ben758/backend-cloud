package com.busfacultativo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showDrawer = currentRoute != "login" && currentRoute != "registro"

    if (!showDrawer) {
        NavHostContainer(navController)
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                ) {
                    DrawerHeader()
                    Spacer(modifier = Modifier.height(16.dp))
                    DrawerItem(
                        label = "Inicio",
                        icon = Icons.Default.Home,
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home")
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        label = "Recargar Saldo",
                        icon = Icons.Default.AddCard,
                        selected = currentRoute == "recarga",
                        onClick = {
                            navController.navigate("recarga")
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        label = "Historial",
                        icon = Icons.Default.History,
                        selected = currentRoute == "historial",
                        onClick = {
                            navController.navigate("historial")
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        label = "Mi Perfil",
                        icon = Icons.Default.Person,
                        selected = currentRoute == "perfil",
                        onClick = {
                            navController.navigate("perfil")
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        label = "Configuración",
                        icon = Icons.Default.Settings,
                        selected = currentRoute == "configuracion",
                        onClick = { /* TODO */ }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    DrawerItem(
                        label = "Cerrar Sesión",
                        icon = Icons.Default.ExitToApp,
                        selected = false,
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            SessionManager.usuario = null
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = when(currentRoute) {
                                    "home" -> "Dashboard"
                                    "recarga" -> "Recarga de Saldo"
                                    "historial" -> "Historial"
                                    "perfil" -> "Mi Perfil"
                                    else -> "Bus Facultativo"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    NavHostContainer(navController)
                }
            }
        }
    }
}

@Composable
fun NavHostContainer(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") { LoginScreen(navController) }
        composable("registro") { PantallaRegistro(navController) }
        composable("home") { HomeScreen(navController) }
        composable("recarga") { RecargaScreen() }
        composable("historial") { HistorialScreen() }
        composable("perfil") { ProfileScreen() }
    }
}

@Composable
fun DrawerHeader() {
    val usuario = SessionManager.usuario
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(24.dp)
            .padding(top = 24.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${usuario?.nombres} ${usuario?.apellidos}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "RU: ${usuario?.codigo_universitario}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${usuario?.saldo_viajes ?: 0} viajes disponibles",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    color: Color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(text = label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedTextColor = color,
            unselectedIconColor = color,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    )
}
