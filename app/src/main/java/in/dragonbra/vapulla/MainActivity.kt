package `in`.dragonbra.vapulla

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import `in`.dragonbra.vapulla.ui.NavigationRoot
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("Created...")
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT))
        setContent {
            val context = LocalContext.current

            // Ask permission to post notifications.
            var hasNotificationPermission by remember {
                val permission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                mutableStateOf(permission == PackageManager.PERMISSION_GRANTED)
            }
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasNotificationPermission = isGranted
            }
            LaunchedEffect(key1 = hasNotificationPermission) {
                if (!hasNotificationPermission) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            VapullaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { innerPadding ->
                        NavigationRoot(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                )
            }
        }
    }
}