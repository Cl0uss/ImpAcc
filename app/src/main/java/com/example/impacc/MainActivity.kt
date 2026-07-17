package com.example.impacc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.impacc.audio.NotificationPolicyHelper
import com.example.impacc.service.ImpAccForegroundService
import com.example.impacc.ui.theme.ImpAccTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ImpAccTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundColor
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ImpAcc",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        MainContent(
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val notificationPolicyHelper = remember {
        NotificationPolicyHelper(context)
    }

    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    }

    var notificationEnabled by remember {
        mutableStateOf(
            preferences.getBoolean(
                KEY_NOTIFICATION_CONTROL_ENABLED,
                false
            )
        )
    }

    var showAudioPermissionDialog by remember {
        mutableStateOf(false)
    }

    var showPolicyPermissionDialog by remember {
        mutableStateOf(false)
    }

    var showAudioPermissionDeniedDialog by remember {
        mutableStateOf(false)
    }

    fun startMonitoringService() {
        if (!notificationPolicyHelper.hasAccess()) {
            showPolicyPermissionDialog = true
            return
        }

        val serviceIntent = Intent(
            context,
            ImpAccForegroundService::class.java
        )

        ContextCompat.startForegroundService(
            context,
            serviceIntent
        )

        notificationEnabled = true

        preferences.edit()
            .putBoolean(
                KEY_NOTIFICATION_CONTROL_ENABLED,
                true
            )
            .apply()
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startMonitoringService()
        } else {
            notificationEnabled = false
            showAudioPermissionDeniedDialog = true
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            ControlButton(
                text = "Screen Control",
                onClick = {
                    val intent = Intent(
                        context,
                        ScreenControlActivity::class.java
                    )

                    context.startActivity(intent)
                }
            )

            NotificationControl(
                enabled = notificationEnabled,
                onChanged = { enabled ->
                    if (enabled) {
                        val audioPermissionGranted =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                        if (audioPermissionGranted) {
                            startMonitoringService()
                        } else {
                            showAudioPermissionDialog = true
                        }
                    } else {
                        val serviceIntent = Intent(
                            context,
                            ImpAccForegroundService::class.java
                        )

                        context.stopService(serviceIntent)

                        notificationEnabled = false

                        preferences.edit()
                            .putBoolean(
                                KEY_NOTIFICATION_CONTROL_ENABLED,
                                false
                            )
                            .apply()
                    }
                }
            )
        }

        if (showAudioPermissionDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAudioPermissionDialog = false
                },
                title = {
                    Text(
                        text = "Microphone permission required"
                    )
                },
                text = {
                    Text(
                        text = "ImpAcc needs microphone permission to detect when another app starts or stops using the microphone. ImpAcc does not record or store audio."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showAudioPermissionDialog = false

                            audioPermissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    ) {
                        Text(
                            text = "Continue"
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAudioPermissionDialog = false
                        }
                    ) {
                        Text(
                            text = "Cancel"
                        )
                    }
                }
            )
        }

        if (showPolicyPermissionDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPolicyPermissionDialog = false
                },
                title = {
                    Text(
                        text = "Do Not Disturb access required"
                    )
                },
                text = {
                    Text(
                        text = "ImpAcc needs Do Not Disturb access to switch the phone to silent mode while the microphone is active."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPolicyPermissionDialog = false
                            notificationPolicyHelper.openSettings()
                        }
                    ) {
                        Text(
                            text = "Open settings"
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPolicyPermissionDialog = false
                        }
                    ) {
                        Text(
                            text = "Cancel"
                        )
                    }
                }
            )
        }

        if (showAudioPermissionDeniedDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAudioPermissionDeniedDialog = false
                },
                title = {
                    Text(
                        text = "Permission denied"
                    )
                },
                text = {
                    Text(
                        text = "Microphone monitoring cannot be enabled without microphone permission."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showAudioPermissionDeniedDialog = false
                        }
                    ) {
                        Text(
                            text = "OK"
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun ControlButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ButtonColor,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NotificationControl(
    enabled: Boolean,
    onChanged: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(4.dp),
        color = ButtonColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Notification Control",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Switch(
                checked = enabled,
                onCheckedChange = onChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF00C853),
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    }
}

private const val PREFERENCES_NAME =
    "impacc_preferences"

private const val KEY_NOTIFICATION_CONTROL_ENABLED =
    "notification_control_enabled"

private val BackgroundColor =
    Color(0xFF121212)

private val ButtonColor =
    Color(0xFF3949AB)

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ImpAccTheme {
        MainScreen()
    }
}