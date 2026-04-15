package com.kinandcarta.create.proxytoggle.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kinandcarta.create.proxytoggle.core.ui.theme.ProxyToggleTheme
import com.kinandcarta.create.proxytoggle.manager.view.screen.BlockAppScreen
import com.kinandcarta.create.proxytoggle.manager.view.screen.ProxyManagerScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationsPermissionIfNeeded()
        setContent {
            val useDarkTheme by viewModel.useDarkTheme.collectAsState()

            @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
            val heightSizeClass = calculateWindowSizeClass(this).heightSizeClass
            val useVerticalLayout = heightSizeClass != WindowHeightSizeClass.Compact

            MainScreen(useDarkTheme = useDarkTheme, useVerticalLayout = useVerticalLayout)
        }
    }

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATIONS_PERMISSION_CODE
        )
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS_PERMISSION_CODE = 1001
    }
}

@Composable
private fun MainScreen(useDarkTheme: Boolean, useVerticalLayout: Boolean) {
    val showBlockDialog = ContextCompat.checkSelfPermission(
        LocalContext.current,
        Manifest.permission.WRITE_SECURE_SETTINGS
    ) == PackageManager.PERMISSION_DENIED

    ProxyToggleTheme(darkTheme = useDarkTheme) {
        if (showBlockDialog) {
            BlockAppScreen()
        } else {
            ProxyManagerScreen(useVerticalLayout = useVerticalLayout)
        }
    }
}
