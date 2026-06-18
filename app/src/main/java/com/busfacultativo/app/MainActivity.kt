package com.busfacultativo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.busfacultativo.app.ui.theme.BusFacultativoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BusFacultativoTheme {
                MainApp()
            }
        }
    }
}
