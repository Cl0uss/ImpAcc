package com.example.impacc

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.impacc.data.ScreenApplication
import com.example.impacc.data.ScreenApplicationStorage
import com.example.impacc.service.ScreenControlAccessibilityService
import com.example.impacc.ui.theme.ImpAccTheme

class ScreenControlActivity : ComponentActivity() {

    private val accessibilityEnabledState =
        mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ImpAccTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundColor
                ) {
                    ScreenControlScreen(
                        accessibilityEnabled =
                            accessibilityEnabledState.value
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        accessibilityEnabledState.value =
            isScreenControlAccessibilityEnabled(this)
    }
}

@Composable
fun ScreenControlScreen(
    accessibilityEnabled: Boolean
) {
    val context = LocalContext.current

    val selectedApplications = remember {
        mutableStateListOf<ScreenApplication>().apply {
            addAll(
                ScreenApplicationStorage.getApplications(context)
            )
        }
    }

    val showAccessibilityDialog = remember {
        mutableStateOf(!accessibilityEnabled)
    }

    fun refreshApplications() {
        selectedApplications.clear()

        selectedApplications.addAll(
            ScreenApplicationStorage.getApplications(context)
        )
    }

    fun requestAccessibilityPermission() {
        showAccessibilityDialog.value = true
    }

    val applicationSettingsLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) {
            refreshApplications()
        }

    val applicationPickerLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                return@rememberLauncherForActivityResult
            }

            val resultIntent =
                result.data
                    ?: return@rememberLauncherForActivityResult

            val applicationName =
                resultIntent.getStringExtra(
                    InstalledApplicationsActivity.EXTRA_APP_NAME
                ) ?: return@rememberLauncherForActivityResult

            val packageName =
                resultIntent.getStringExtra(
                    InstalledApplicationsActivity.EXTRA_PACKAGE_NAME
                ) ?: return@rememberLauncherForActivityResult

            ScreenApplicationStorage.addApplication(
                context = context,
                name = applicationName,
                packageName = packageName
            )

            refreshApplications()

            val settingsIntent = Intent(
                context,
                ApplicationSettingsActivity::class.java
            ).apply {
                putExtra(
                    ApplicationSettingsActivity.EXTRA_PACKAGE_NAME,
                    packageName
                )
            }

            applicationSettingsLauncher.launch(
                settingsIntent
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Screen Control",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (!accessibilityEnabled) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 32.dp,
                        vertical = 8.dp
                    ),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF5D4037)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Screen Control is disabled",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Enable the accessibility service to show overlays over selected applications.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Button(
                        onClick = {
                            requestAccessibilityPermission()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Enable Screen Control"
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 32.dp,
                        vertical = 8.dp
                    ),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF1B5E20)
            ) {
                Text(
                    text = "Screen Control is active",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {
                if (!accessibilityEnabled) {
                    requestAccessibilityPermission()
                    return@Button
                }

                val pickerIntent = Intent(
                    context,
                    InstalledApplicationsActivity::class.java
                )

                applicationPickerLauncher.launch(
                    pickerIntent
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(60.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonColor,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Add application",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        if (selectedApplications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No applications added",
                    color = Color.Gray,
                    fontSize = 17.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    items = selectedApplications,
                    key = { application ->
                        application.packageName
                    }
                ) { application ->
                    ScreenApplicationRow(
                        application = application,
                        onOpenSettings = {
                            if (!accessibilityEnabled) {
                                requestAccessibilityPermission()
                                return@ScreenApplicationRow
                            }

                            val settingsIntent = Intent(
                                context,
                                ApplicationSettingsActivity::class.java
                            ).apply {
                                putExtra(
                                    ApplicationSettingsActivity.EXTRA_PACKAGE_NAME,
                                    application.packageName
                                )
                            }

                            applicationSettingsLauncher.launch(
                                settingsIntent
                            )
                        },
                        onRemove = {
                            ScreenApplicationStorage.removeApplication(
                                context = context,
                                packageName = application.packageName
                            )

                            refreshApplications()
                        }
                    )

                    HorizontalDivider(
                        color = Color(0xFF303030)
                    )
                }
            }
        }
    }

    if (showAccessibilityDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showAccessibilityDialog.value = false
            },
            title = {
                Text(
                    text = "Accessibility permission required"
                )
            },
            text = {
                Text(
                    text = "ImpAcc needs its Screen Control accessibility service enabled to detect when a selected application is active and display click-through screen overlays. ImpAcc does not read or store text entered in other applications."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccessibilityDialog.value = false

                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_ACCESSIBILITY_SETTINGS
                                )
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_SETTINGS
                                )
                            )
                        }
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
                        showAccessibilityDialog.value = false
                    }
                ) {
                    Text(
                        text = "Cancel"
                    )
                }
            }
        )
    }
}

@Composable
private fun ScreenApplicationRow(
    application: ScreenApplication,
    onOpenSettings: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    val applicationIcon = remember(
        application.packageName
    ) {
        try {
            context.packageManager.getApplicationIcon(
                application.packageName
            )
        } catch (_: Exception) {
            context.applicationInfo.loadIcon(
                context.packageManager
            )
        }
    }

    val iconPainter = rememberDrawablePainter(
        drawable = applicationIcon
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onOpenSettings
            )
            .padding(
                horizontal = 20.dp,
                vertical = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = iconPainter,
            contentDescription = application.name,
            modifier = Modifier.size(48.dp)
        )

        Spacer(
            modifier = Modifier.size(16.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = application.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = buildSettingsSummary(application),
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        Text(
            text = "Remove",
            modifier = Modifier
                .clickable(
                    onClick = onRemove
                )
                .padding(12.dp),
            color = Color(0xFFFF5252),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun buildSettingsSummary(
    application: ScreenApplication
): String {
    return "U ${application.upperPart}px · " +
            "D ${application.lowerPart}px · " +
            "L ${application.leftPart}px · " +
            "R ${application.rightPart}px · " +
            "${application.transparency}%"
}

@Composable
private fun rememberDrawablePainter(
    drawable: Drawable
): Painter {
    return remember(drawable) {
        BitmapPainter(
            drawable.toBitmap().asImageBitmap()
        )
    }
}

private fun isScreenControlAccessibilityEnabled(
    context: Context
): Boolean {
    val accessibilityManager =
        context.getSystemService(
            AccessibilityManager::class.java
        )

    val enabledServices =
        accessibilityManager
            .getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )

    val expectedServiceName =
        ScreenControlAccessibilityService::class.java.name

    return enabledServices.any { serviceInfo ->
        val service = serviceInfo.resolveInfo.serviceInfo

        service.packageName == context.packageName &&
                service.name == expectedServiceName
    }
}

private val BackgroundColor =
    Color(0xFF121212)

private val ButtonColor =
    Color(0xFF3949AB)